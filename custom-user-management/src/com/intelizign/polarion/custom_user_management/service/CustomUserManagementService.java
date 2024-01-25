package com.intelizign.polarion.custom_user_management.service;
 
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
 
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.persistence.UnresolvableObjectException;
import com.polarion.alm.projects.model.IUser;
import com.polarion.platform.persistence.model.IPObjectList;
 
public class CustomUserManagementService {
 
    private static final ITrackerService trackerService = (ITrackerService) PlatformContext.getPlatform()
            .lookupService(ITrackerService.class);
    private static final Logger log = Logger.getLogger(CustomUserManagementService.class);
    private Gson gson = new Gson();
 
    public CustomUserManagementService() {
 
    }
 
    public void getUserDetails(HttpServletRequest req, HttpServletResponse resp) {
        try{
        	System.out.println("Get User Details Method in Service Page");
        	PrintWriter out = resp.getWriter();
        	String projectId = req.getParameter("projectId");
            System.out.println("Project Id in service Page is :" + projectId);
            IProject trackerProject = trackerService.getProjectsService().getProject(projectId);
            IPObjectList<IUser> userList = trackerService.getProjectsService().getProjectUsers(trackerProject);
            
            // Initialize userMap for each request
            Map<String, String> userMap = new HashMap<>();
 
            for (IUser user : userList) {
                try {
                    userMap.put(user.getId(), user.getName());
                } catch (UnresolvableObjectException e) {
                    log.error("Skipping entry due to UnresolvableObjectException: " + e.getMessage());
                } catch (Exception e) {
                    log.error("Exception is" + e.getMessage());
                    continue;
                }
            }
 
            // Serialize userMap directly if needed
            // String jsonProjectResponse = gson.toJson(userMap);
 
            // Wrap userMap in a JsonObject if needed
          //  System.out.println("UserMap is" + userMap);
            JsonObject responseData = new JsonObject();
            responseData.add("userMap", gson.toJsonTree(userMap));
            String jsonProjectResponse = gson.toJson(responseData);
 
            resp.setContentType("application/json");
            out.write(jsonProjectResponse);
        } catch (IOException e) {
            log.error("IOException occurred: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            log.error("Unhandled exception occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}