package com.contoso;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.logger.DefaultLogger;
import com.microsoft.graph.logger.LoggerLevel;
import com.microsoft.graph.models.extensions.*;
import com.microsoft.graph.options.Option;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.extensions.*;

/**
 * Graph
 */
public class Graph {

    private static IGraphServiceClient graphClient = null;
    private static SimpleAuthProvider authProvider = null;

    private static void ensureGraphClient(String accessToken) {
        if (graphClient == null) {
            // Create the auth provider
            authProvider = new SimpleAuthProvider(accessToken);

            // Create default logger to only log errors
            DefaultLogger logger = new DefaultLogger();
            logger.setLoggingLevel(LoggerLevel.ERROR);

            // Build a Graph client
            graphClient = GraphServiceClient.builder()
                    .authenticationProvider(authProvider)
                    .logger(logger)
                    .buildClient();
        }
    }

    public static User getUser(String accessToken) {
        ensureGraphClient(accessToken);

        // GET /me to get authenticated user
        User me = graphClient
                .me()
                .buildRequest()
                .get();

        return me;
    }

    public static List<Event> getEvents(String accessToken) {
        ensureGraphClient(accessToken);

        // Use QueryOption to specify the $orderby query parameter
        final List<Option> options = new LinkedList<Option>();
        // Sort results by createdDateTime, get newest first
        options.add(new QueryOption("orderby", "createdDateTime DESC"));

        // GET /me/events
        IEventCollectionPage eventPage = graphClient
                .me()
                .events()
                .buildRequest(options)
                .select("subject,organizer,start,end")
                .get();

        return eventPage.getCurrentPage();
    }

    /**
     * return a List of DriveItems for the root directory of "my" OneDrive
     */
    public static List<DriveItem> getDriveItems(String accessToken) {
        ensureGraphClient(accessToken);

        // Use QueryOption to specify the $orderby query parameter
        final List<Option> options = new LinkedList<Option>();
        // Sort results by createdDateTime, get newest first
        options.add(new QueryOption("orderby", "createdDateTime DESC"));

        // GET /me/drive/root/children
        IDriveItemCollectionPage childrenPage = graphClient
                .me()
                .drive()
                .root()
                .children()
                .buildRequest()
                .get();


        return childrenPage.getCurrentPage();
    }

    /**
     *  getItemChildren - get a list of items which are children of itemId
     * @param accessToken - the accessToken to access my OneDrive
     * @param itemId - the identifier of the DriveItem
     * @return List of DriveItems which are children of the DriveItem
     */
    public static List<DriveItem> getItemChildren(String accessToken,String itemId) {
        ensureGraphClient(accessToken);

        // Use QueryOption to specify the $orderby query parameter
        final List<Option> options = new LinkedList<Option>();
        // Sort results by createdDateTime, get newest first
        options.add(new QueryOption("orderby", "createdDateTime DESC"));

        // GET /me/drive/root/children
        IDriveItemCollectionPage childrenPage = graphClient
                .me()
                .drive()
                .items(itemId)
                .children()
                .buildRequest()
                .top(600)
                .get();


        return childrenPage.getCurrentPage();
    }

    public static String getEmbedLink(String accessToken, String itemId) {
        ensureGraphClient(accessToken);

        Permission permission = null;
        //System.out.println("getEmbedLink: about to get permission");
        //int i = 0;

        //while (permission == null && i < 6) {
            try {
                permission = graphClient
                    .me().drive().items(itemId)
                    .createLink("embed", "anonymous")
                    .buildRequest()
                    .post();
                //i=0;
            } catch (GraphServiceException e) {
                // may need to serialize the GraphServiceException into json in order to get the request header for Retry-After
                System.out.println("printing GraphServiceException error message: " + e.getMessage(true));
                System.out.println("retrying after 60 seconds");
                //i++;
            } catch (ClientException ce) {
                System.out.println("printing ClientException error message: " + ce.getMessage());
                System.out.println("retrying after 600 seconds");
                //i++;
            }
            //if (i>0) {
            //    try {
            //        TimeUnit.SECONDS.sleep(600);
            //    } catch (InterruptedException ie) { ie.printStackTrace();}
            //}
        //}
        //System.out.println("getEmbedLink: got permission");

        /*IPermissionCollectionPage permissionPage = graphClient
                .me()
                .drive()
                .items(itemId)
                .permissions()
                .buildRequest()
                .get();

        List<Permission> permissions = permissionPage.getCurrentPage();
        System.out.println("number of permissions: " + permissions.size());*/
//        System.out.println("link type = " + permission.link.type);
//        System.out.println(permission.link.scope);
//        System.out.println(permission.link.oDataType);
//        System.out.println(permission.link.webUrl);
        return permission.link.webUrl;
    }
    public static void diagnoseVideoLink(String accessToken, String itemId) {
        ensureGraphClient(accessToken);

        IPermissionCollectionPage permissionsPage = graphClient
                .me()
                .drive()
                .items(itemId)
                .permissions()
                .buildRequest()
                .get();

        List<Permission> permissions = permissionsPage.getCurrentPage();
        for (Permission permission : permissions) {
            System.out.println("  Potential url: (" + permission.link.type + ") " + permission.link.webUrl);
        }
    }
    public static List<ThumbnailSet> getThumbnails(String accessToken, String itemId) {
        ensureGraphClient(accessToken);

        IThumbnailSetCollectionPage thumbnailPage = graphClient
                .me()
                .drive()
                .items(itemId)
                .thumbnails()
                .buildRequest()
                .get();
        return thumbnailPage.getCurrentPage();
    }

}
