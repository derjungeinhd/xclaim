package codes.wasabi.xclaim.api.dynmap;

import codes.wasabi.xclaim.XClaim;
import codes.wasabi.xclaim.api.Claim;
import codes.wasabi.xclaim.util.hull.ConvexHull;
import codes.wasabi.xclaim.util.hull.Point;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.dynmap.bukkit.DynmapPlugin;
import org.dynmap.markers.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

public class DynmapInterface {

    private final DynmapPlugin dynmap;

    public DynmapInterface(DynmapPlugin plugin) {
        dynmap = plugin;
        XClaim.logger.log(Level.INFO, "Hooked into Dynmap version " + dynmap.getDynmapVersion());
    }

    public String getVersion() {
        return dynmap.getDynmapVersion();
    }

    private final Map<UUID, Color> colorMap = new HashMap<>();
    public @NotNull Color getClaimColor(@NotNull Claim claim) {
        OfflinePlayer op = claim.getOwner();
        UUID uuid = op.getUniqueId();
        Color color = colorMap.get(uuid);
        if (color == null) {
            Random random = new Random(op.getUniqueId().hashCode());
            int red = random.nextInt(256);
            int green = random.nextInt(256);
            int blue = random.nextInt(256);
            color = new Color(red, green, blue);
            colorMap.put(uuid, color);
        }
        return color;
    }

    private @NotNull String getClaimIdentifier(@NotNull Claim claim) {
        return new String(Base64.getEncoder().encode(claim.getName().getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }

    public @Nullable AreaMarker getMarker(@NotNull Claim claim) {
        World w = claim.getWorld();
        if (w == null) return null;
        MarkerSet set = dynmap.getMarkerAPI().getMarkerSet(MarkerSet.DEFAULT);
        String identifier = "claim_marker_" + getClaimIdentifier(claim);
        AreaMarker marker = set.findAreaMarker(identifier);
        if (marker == null) {
            marker = set.createAreaMarker(identifier, claim.getName(), false, w.getName(), new double[]{ 0, 0 }, new double[]{ 0, 0 }, false);
            Color color = getClaimColor(claim);
            int rgb = color.getRGB();
            marker.setFillStyle(0.5d, rgb);
            marker.setLineStyle(3, 0.8d, rgb);
            String ownerName;
            OfflinePlayer op = claim.getOwner();
            Player ply = op.getPlayer();
            if (ply != null) {
                ownerName = PlainTextComponentSerializer.plainText().serializeOr(ply.displayName(), ply.getName());
            } else {
                ownerName = op.getName();
                if (ownerName == null) ownerName = op.getUniqueId().toString();
            }
            marker.setDescription("Owner: " + ownerName);
        }
        return marker;
    }

    public void updateMarker(@NotNull AreaMarker marker, @NotNull Claim claim) {
        int minHeight = 0;
        World w = claim.getWorld();
        if (w != null) minHeight = w.getMinHeight();
        List<Point> points = new ArrayList<>();
        for (Chunk c : claim.getChunks()) {
            Block cornerBlock = c.getBlock(0, minHeight, 0);
            int cornerX = cornerBlock.getX();
            int cornerZ = cornerBlock.getZ();
            points.add(new Point(cornerX        , cornerZ        ));
            points.add(new Point(cornerX + 16, cornerZ        ));
            points.add(new Point(cornerX        , cornerZ + 16));
            points.add(new Point(cornerX + 16, cornerZ + 16));
        }
        points = ConvexHull.makeHull(points);
        double[] xLocations = new double[points.size()];
        double[] zLocations = new double[points.size()];
        for (int i=0; i < points.size(); i++) {
            Point point = points.get(i);
            xLocations[i] = point.x;
            zLocations[i] = point.y;
        }
        marker.setCornerLocations(xLocations, zLocations);
    }

}