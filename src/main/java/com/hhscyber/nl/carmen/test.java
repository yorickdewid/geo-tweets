/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hhscyber.nl.carmen;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhscyber.nl.carmen.demo.LocationResolverDemo;
import com.hhscyber.nl.carmen.types.Location;
import com.hhscyber.nl.carmen.utils.CommandLineUtilities;
import com.hhscyber.nl.carmen.utils.Utils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.commons.cli.Option;
import org.apache.log4j.Logger;

/**
 *
 * @author dev
 */
public class test {

    protected static Logger logger = Logger.getLogger(LocationResolverDemo.class);
    protected static List<Option> options = new LinkedList<Option>();

    private static void createCommandLineOptions() {
        Utils.registerOption(options, "input_file", "String", true, "A file containing the tweets to locate with geolocation field.");
        Utils.registerOption(options, "output_file", "String", true, "A file to write geolocated tweets.");
    }
    
    public void testT()
    {
        String[] ids = TimeZone.getAvailableIDs();
       for (String id : ids) {
            TimeZone tz = TimeZone.getTimeZone(id);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(tz.getRawOffset());
            System.out.println(seconds);
        }
    }
    
    /**
     * TEST timezone function
     * @param args
     * @throws ParseException
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    public static void main(String[] args) throws ParseException, FileNotFoundException, IOException, ClassNotFoundException {
        // Parse the command line.

        String[] manditory_args = {"input_file"};
        createCommandLineOptions();
        CommandLineUtilities.initCommandLineParameters(args, test.options, manditory_args);

        // Get options
        String inputFile = CommandLineUtilities.getOptionValue("input_file");
        System.out.println("input" + inputFile);
        String outputFile = null;
        if (CommandLineUtilities.hasArg("output_file")) {
            outputFile = CommandLineUtilities.getOptionValue("output_file");
        }

        logger.info("Creating LocationResolver.");
        LocationResolver resolver = LocationResolver.getLocationResolver();

        int numResolved;
        int total;
        try (Scanner scanner = Utils.createScanner(inputFile)) {
            Writer writer = null;
            if (outputFile != null) {
                writer = Utils.createWriter(outputFile);
                if (writer != null) {
                    System.out.println("Not null");
                }
                logger.info("Saving geolocated tweets to: " + outputFile);
            }
            ObjectMapper mapper = new ObjectMapper();
            numResolved = 0;
            total = 0;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                JSONObject json = (JSONObject) JSONValue.parse(line);
                JSONArray jry = (JSONArray) json.get("statuses");
                if (jry == null) {
                    return;
                }

                for (Object jry1 : jry) {
                    @SuppressWarnings("unchecked")
                    HashMap<String, Object> tweet = (HashMap<String, Object>) mapper.readValue(jry1.toString(), Map.class);

                    total++;
                    Location location = resolver.resolveLocationUsingTimeZone(tweet);

                    if (location != null) {
                        logger.debug("Found location: " + location.toString());
                        numResolved++;
                    }
                    if (writer != null) {
                        if (location != null) {
                            tweet.put("location", Location.createJsonFromLocation(location));
                        }
                        try {
                            mapper.writeValue(writer, tweet);
                            writer.write("\n");
                        } catch (IOException e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
            }
            if (writer != null) {
                writer.close();
            }
            scanner.close();
        }

        logger.info("Resolved locations for " + numResolved + " of " + total + " tweets.");
    }
}
