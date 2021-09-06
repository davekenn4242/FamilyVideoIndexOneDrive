package com.contoso;

import com.microsoft.graph.models.extensions.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.TimeUnit;

import java.io.IOException;
import java.util.List;

/**
 * Kennedy Family Video App
 *
 * Reads video files from OneDrive account after authenticating.  Then generates rss files by year with metadata from the videos.
 *
 */
public class App {
    public static void main(String[] args) {
        System.out.println("Kennedy Family Video App");
        System.out.println();
// Load OAuth settings
        final Properties oAuthProperties = new Properties();
        try {
            oAuthProperties.load(App.class.getResourceAsStream("oAuth.properties"));
        } catch (IOException e) {
            System.out.println("Unable to read OAuth configuration. Make sure you have a properly formatted oAuth.properties file. See README for details.");
            return;
        }

        final String appId = oAuthProperties.getProperty("app.id");
        final String[] appScopes = oAuthProperties.getProperty("app.scopes").split(",");

        // Get an access token
        Authentication.initialize(appId);
        final String accessToken = Authentication.getUserAccessToken(appScopes);

        // Greet the user
        User user = Graph.getUser(accessToken);
        System.out.println("Welcome " + user.displayName);
        System.out.println();

        Scanner input = new Scanner(System.in);

        int choice = -1;

        while (choice != 0) {
            System.out.println("Please choose one of the following options:");
            System.out.println("0. Exit");
            System.out.println("1. Display access token");
            System.out.println("2. List calendar events");
            System.out.println("3. Generate RSS files from OneDrive videos");

            try {
                choice = input.nextInt();
            } catch (InputMismatchException ex) {
                // Skip over non-integer input
                input.nextLine();
            }

            // Process user choice
            switch(choice) {
                case 0:
                    // Exit the program
                    System.out.println("Goodbye...");
                    break;
                case 1:
                    // Display access token
                    System.out.println("Access token: " + accessToken);
                    break;
                case 2:
                    // List the calendar
                    listCalendarEvents(accessToken);
                    break;
                case 3:
                    // Generate RSS Files
                    listRootChildren(accessToken);
                    break;
                default:
                    System.out.println("Invalid choice");
            }
        }

        input.close();
    }
    private static String formatDateTimeTimeZone(DateTimeTimeZone date) {
        LocalDateTime dateTime = LocalDateTime.parse(date.dateTime);

        return dateTime.format(
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)) +
                " (" + date.timeZone + ")";
    }
    private static void listCalendarEvents(String accessToken) {
        // Get the user's events
        List<Event> events = Graph.getEvents(accessToken);

        System.out.println("Events:");

        for (Event event : events) {
            System.out.println("Subject: " + event.subject);
            System.out.println("  Organizer: " + event.organizer.emailAddress.name);
            System.out.println("  Start: " + formatDateTimeTimeZone(event.start));
            System.out.println("  End: " + formatDateTimeTimeZone(event.end));
        }

        System.out.println();
    }
    private static void listRootChildren(String accessToken) {
        // Get the user's root children on OneDrive
        List<DriveItem> items = Graph.getDriveItems(accessToken);

        //System.out.println("DriveItems:");

        try {
            for (DriveItem item : items) {
                if (item.name.equals("Videos")) {
                    System.out.println("Id: " + item.id);
                    System.out.println("  Name: " + item.name);
                    //System.out.println("  Description: " + item.description);
                    //System.out.println("  End: " + item.+ formatDateTimeTimeZone(event.end));
                    List<DriveItem> subFolders = Graph.getItemChildren(accessToken, item.id);
                    System.out.println(subFolders.size() + " folders of videos");
                    HashMap<String, BufferedWriter> myWriters = new HashMap<String, BufferedWriter>();

                    for (DriveItem folder : subFolders) {

                        System.out.println("Folder: " + folder.name);

                        String year = folder.name.substring(0, 4);
                        if (year.equals("2012")) {
                            BufferedWriter myWriter = myWriters.get(year);
                            if (myWriter == null) {
                                myWriter = initializeWriter(year);
                                myWriters.put(year, myWriter);
                            }

//                          String datePattern = "E, dd MMM yyyy HH:mm:ss z";
//                          SimpleDateFormat simpleDateFormat = new SimpleDateFormat(datePattern);
//                          String folderDate = simpleDateFormat.format(new SimpleDateFormat("yyyy-MM-dd").parse(folder.name));

                            //System.out.println("Folder Id: " + folder.id);
                            System.out.println("=====================");
                            System.out.println("Processing Folder: " + folder.name);
                            List<DriveItem> videos = Graph.getItemChildren(accessToken, folder.id);
                            for (DriveItem video : videos) {
                                if (video.name.matches(".*mp4") || video.name.matches(".*MP4")) {
                                    outputVideo(video, folder.name, accessToken, myWriter);
                                    //TimeUnit.SECONDS.sleep(5);
                                }
                            }
                        }
                    }

                    closeWriters(myWriters);

                }
            }
        } catch (IOException e) {
            System.out.println(e);
//        } catch (ParseException pe) {
//            System.err.println(pe);
//        } catch (InterruptedException ie) {
//            System.out.println(ie);
        }

        System.out.println();
    }

    private static BufferedWriter initializeWriter(String year) throws IOException {
        BufferedWriter myWriter = new BufferedWriter(new FileWriter(year + ".rss", false));
        myWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        myWriter.newLine();
        myWriter.write("<rss xmlns:media=\"http://search.yahoo.com/mrss/\" version=\"2.0\">");
        myWriter.newLine();
        myWriter.write("<channel>");
        myWriter.newLine();
        myWriter.write("  <title>"+year+"-video-list</title>");
        myWriter.newLine();
        myWriter.write("  <link />");
        myWriter.newLine();
        myWriter.write("  <description>Kennedy Family Videos from the Year "+year+"</description>");
        myWriter.newLine();
        myWriter.write("  <language>en-us</language>");
        myWriter.newLine();

        myWriter.write("  <pubDate>Wed, 11 Nov 2015 20:30:54 GMT</pubDate>");
        myWriter.newLine();

        myWriter.write("  <image>");
        myWriter.newLine();
        myWriter.write("    <title>"+year+"-Kennedy-Family-Video-Feed</title>");
        myWriter.newLine();
        myWriter.write("    <url>http://s2.content.video.llnw.net/lovs/images-prod/59021fabe3b645968e382ac726cd6c7b/channel/1cfd09ab38e54f48be8498e0249f5c83/S9O.600x600.png</url>");
        myWriter.newLine();
        myWriter.write("    <width>-1</width>");
        myWriter.newLine();
        myWriter.write("    <height>-1</height>");
        myWriter.newLine();
        myWriter.write("  </image>");
        myWriter.newLine();

        return myWriter;
    }
    private static void outputVideo(DriveItem video, String folderDate, String accessToken, BufferedWriter myWriter) throws IOException {
        //System.out.println("outputVideo()");
        //System.out.println("getVideoLink()");
        System.out.println(" Processing Video: " + video.name);
        String link = getVideoLink(accessToken,video);
        if (!link.matches(".*embed.*")) {

            System.out.println("*** ERROR Creating link ***");
            System.out.println("    Video name: " + video.name);
            System.out.println("      Video Embed Link: " + link);

            diagnoseVideoLink(accessToken,video);
        }
        link = link.replaceFirst("embed","download");
        link = link.replaceAll("&","&amp;");

        //System.out.println("getThumbnail()");
        String thumbnailLink = getThumbnail(accessToken,video);
        thumbnailLink = thumbnailLink.replaceAll("&","&amp;");

        myWriter.write("  <item>");
        myWriter.newLine();
        myWriter.write("    <title>"+video.name+"</title>");
        myWriter.newLine();
        myWriter.write("    <description>"+video.description+"</description>");
        myWriter.newLine();
        myWriter.write("    <pubDate>"+folderDate+"</pubDate>");
        myWriter.newLine();
        myWriter.write("    <guid isPermaLink=\"false\">"+video.id+"</guid>");
        myWriter.newLine();
        if (video.video != null) {
            myWriter.write("    <media:content duration=\"" + video.video.duration + "\" fileSize=\"" + video.size + "\" height=\"" + video.video.height + "\" type=\"video/mp4\" width=\"" + video.video.width + "\" isDefault=\"true\" url=\"" + link + "\">");
        } else {
            myWriter.write("    <media:content fileSize=\"" + video.size + "\" type=\"video/mp4\" isDefault=\"true\" url=\"" + link + "\">");
        }
        myWriter.newLine();
        myWriter.write("      <media:description>"+video.description+"</media:description>");
        myWriter.newLine();
        myWriter.write("      <media:keywords>episode 39, roku recommends, showtime, the affair</media:keywords>");
        myWriter.newLine();
        myWriter.write("      <media:thumbnail url=\"" + thumbnailLink + "\" />");
        myWriter.newLine();
        myWriter.write("      <media:title>"+video.name+"</media:title>");
        myWriter.newLine();
        myWriter.write("    </media:content>");
        myWriter.newLine();
        myWriter.write("  </item>");
        myWriter.newLine();
        myWriter.flush();
    }
    private static String getVideoLink(String accessToken, DriveItem video) {
        String embedLink = Graph.getEmbedLink(accessToken,video.id);
        return embedLink;
    }
    private static void diagnoseVideoLink(String accessToken, DriveItem video) {
        Graph.diagnoseVideoLink(accessToken,video.id);
    }
    private static String getThumbnail(String accessToken, DriveItem video) {
        List<ThumbnailSet> thumbnailSets = Graph.getThumbnails(accessToken,video.id);
        for (ThumbnailSet thumbnailSet : thumbnailSets) {
            //System.out.println("Thumbnail id: " + thumbnailSet.id);
            //System.out.println(thumbnailSet.large.url);
            return thumbnailSet.large.url;
        }
        return "http://s2.content.video.llnw.net/lovs/images-prod/59021fabe3b645968e382ac726cd6c7b/media/e92ddcb71f154e6897f5900471c54992/aUf.540x304.jpeg";
    }
    private static void closeWriters(HashMap<String,BufferedWriter> writers) throws IOException {
        writers.forEach((String key, BufferedWriter bw)->{
            try {
                bw.write("</channel>");
                bw.newLine();
                bw.write("</rss>");
                bw.close();
            } catch (IOException e) {
                System.err.println(e);
            }
        });
    }
}
