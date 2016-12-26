package io.github.dchaosknight.friskstick.data;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReportData {

    private static List<List<UUID>> reports = new ArrayList<>();
    private static List<String> dates = new ArrayList<>();

    /**
     * Gets the list of reports known to the plugin. Each report is a list of exactly two elements. The first element
     * is the UUID of the player who submitted the report. The second element is the UUID of the player who was
     * reported.
     * @return A list of all known reports
     */
    public static List<List<UUID>> getAllReports() {

        return reports;

    }

    /**
     * Adds a report to the list. Will not add a report if it is a duplicate (a report is considered a duplicate if
     * it has the same {@code reporterUUID} and {@code reportedUUID} as another report in the list).
     * 
     * @param reporterUUID The UUID of the player making the report
     * @param reportedUUID The UUID of the player being reported
     * @return {@code true} if the report was added, {@code false} if the report was a duplicate
     */
    public static boolean addReport(UUID reporterUUID, UUID reportedUUID) {

        List<UUID> report = new ArrayList<>();

        report.add(reporterUUID);
        report.add(reportedUUID);

        if (!reports.contains(report)) {

            reports.add(report);
            dates.add(new SimpleDateFormat("yyyy-MM-dd kk:mm:ss").format(new Date()));
            return true;

        }

        return false;

    }

    /**
     * Removes a report from the list.
     *
     * @param index The index of the report to be deleted
     * @return {@code true} if the report was deleted, {@code false} if the index was either out of bounds or a negative integer
     */
    public static boolean removeReport(int index) {

        if (index >= reports.size() || index < 0) {

            return false;

        }

        reports.remove(index);
        dates.remove(index);

        return true;

    }

    /**
     * Gets the formatted date of a report.
     *
     * @param format The date/time format to use (see {@link java.text.SimpleDateFormat} for more detail)
     * @param index The index of the report whose date is being retrieved
     * @return A string containing the formatted date
     * @throws ParseException if the date of the report cannot be parsed
     */
    public static String getFormattedDateOfReport(String format, int index) throws ParseException {

        Date timestamp = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss").parse(dates.get(index));
        return new SimpleDateFormat(format).format(timestamp);

    }

    /**
     * Loads reports listed in the {@code reports.conf} file into memory.
     *
     * @param reportFile The {@link Path} to the report file
     * @throws IOException if the report file could not be opened
     * @throws ObjectMappingException if the values in the file are not valid UUIDs
     */
    public static void loadReportsFromFile(Path reportFile) throws IOException, ObjectMappingException {

        ConfigurationNode root = HoconConfigurationLoader.builder().setPath(reportFile).build().load();

        for (int i = 0; ; i++) {

            ConfigurationNode reportNode = root.getNode("report" + i);

            if (reportNode.isVirtual()) {

                break;

            }

            List<UUID> report = new ArrayList<>();

            report.add(reportNode.getNode("reporter").getValue(TypeToken.of(UUID.class)));
            report.add(reportNode.getNode("reported").getValue(TypeToken.of(UUID.class)));

            reports.add(report);
            dates.add(reportNode.getNode("date").getString());

        }

    }

    /**
     * Saves reports to the {@code reports.conf} file.
     *
     * @param reportFile The {@link Path} to the report file
     * @throws IOException if the report file could not be saved
     */
    public static void saveReportsToFile(Path reportFile) throws IOException {

        ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder().setPath(reportFile).build();

        ConfigurationNode root = loader.createEmptyNode();

        reports.forEach(report -> {

            ConfigurationNode entry = root.getNode("report" + reports.indexOf(report));

            entry.getNode("reporter").setValue(report.get(0).toString());
            entry.getNode("reported").setValue(report.get(1).toString());
            entry.getNode("date").setValue(dates.get(reports.indexOf(report)));

        });

        loader.save(root);

    }

}
