package com.intelizign.polarion.custom_user_management.service;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.intelizign.polarion.custom_user_management.model.CustomDetailsPojo;
import com.intelizign.polarion.custom_user_management.model.CustomEnumerationPojo;
import com.intelizign.polarion.custom_user_management.model.ScriptConditionPojo;
import com.intelizign.polarion.custom_user_management.model.ScriptCountPojo;
import com.intelizign.polarion.custom_user_management.model.ScriptFunctionPojo;
import com.polarion.alm.projects.model.IFolder;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IRichPage;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.workflow.config.IAction;
import com.polarion.alm.tracker.workflow.config.IOperation;
import com.polarion.alm.tracker.workflow.config.IWorkflowConfig;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.persistence.ICustomFieldsService;
import com.polarion.platform.persistence.IEnumObjectFactory;
import com.polarion.platform.persistence.UnresolvableObjectException;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.subterra.base.data.model.ICustomField;


public class CustomUserManagementService {

	private static final ITrackerService trackerService = (ITrackerService) PlatformContext.getPlatform()
			.lookupService(ITrackerService.class);
	private static final Logger log = Logger.getLogger(CustomUserManagementService.class);
	private List<CustomDetailsPojo> customDetailsList = new ArrayList<>();
	private List<ScriptConditionPojo> scriptConditionList = new ArrayList<>();
	private List<ScriptCountPojo> scriptCountList = new ArrayList<>();
	private List<ScriptFunctionPojo> scriptFunctionList = new ArrayList<>();
	private List<ScriptCountPojo> scriptFunctionCountList = new ArrayList<>();
	private Map<String, String> hookMap = new HashMap<>();
	private Set<CustomEnumerationPojo> customEnumerationList = new HashSet<>();
	private Gson gson = new Gson();
	private Map<String, String> projectMap = new HashMap<String, String>();
	private Map<String, Object> responseData = new HashMap<>();

	public CustomUserManagementService() {

	}

	// Get all Project From the server
	public void getProjectList(HttpServletRequest req, HttpServletResponse resp) throws UnresolvableObjectException {
		try {
			PrintWriter out = resp.getWriter();
			IPObjectList<IProject> getProjectList = trackerService.getProjectsService().searchProjects("", "id");
			for (IProject pro : getProjectList) {
				try {
					projectMap.put(pro.getId(), pro.getName());
				} catch (UnresolvableObjectException e) {
					log.error("Skipping entry due to UnresolvableObjectException: " + e.getMessage());
				} catch (Exception e) {
					log.error("Exception is" + e.getMessage());
					continue;
				}
			}
			getPrePostSaveScript(responseData);
			responseData.put("projectMap", projectMap);
			String projectResponsObj = gson.toJson(responseData);
			out.println(projectResponsObj);
		} catch (Exception e) {
			System.out.println("Error Message is" + e.getMessage());
			e.printStackTrace();
		}
	}

	// Get Customization Details From the respective Project
	public void getCustomizationDetails(HttpServletRequest req, HttpServletResponse resp)
			throws UnresolvableObjectException {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			String projectId = req.getParameter("projectId");
			ITrackerProject trackerPro = trackerService.getTrackerProject(projectId);
			if (!(customDetailsList.isEmpty())) {
				customDetailsList.clear();
			}
			if (!(scriptConditionList.isEmpty())) {
				scriptConditionList.clear();
			}
			if (!(scriptFunctionList.isEmpty())) {
				scriptFunctionList.clear();
			}
			if (!(scriptCountList.isEmpty())) {
				scriptCountList.clear();
			}
			if (!(scriptFunctionCountList.isEmpty())) {
				scriptFunctionCountList.clear();
			}
			if (!(customEnumerationList.isEmpty())) {
				customEnumerationList.clear();
			}
			List<ITypeOpt> workItemTypeEnum = trackerService.getTrackerProject(projectId).getWorkItemTypeEnum()
					.getAvailableOptions("type");
			for (ITypeOpt wiTypeEnum : workItemTypeEnum) {
				try {

					getCustomFieldDetails(trackerPro, wiTypeEnum);
					getCustomWorkFlowScript(trackerPro, wiTypeEnum);
					getCustomEnumerationDetails(trackerPro, wiTypeEnum);
				} catch (UnresolvableObjectException e) {
					log.error("Skipping entry due to UnresolvableObjectException: " + e.getMessage());
				} catch (Exception e) {
					log.error("Exception is" + e.getMessage());
					continue;
				}
			}
			String customDetailsJson = objectMapper.writeValueAsString(customDetailsList);
			System.out.println("Script Condition List is" + scriptConditionList + "\n");
			String scriptConditionJson = objectMapper.writeValueAsString(scriptConditionList);
			String scriptCountJson = objectMapper.writeValueAsString(scriptCountList);
			String scriptFunctionJson = objectMapper.writeValueAsString(scriptFunctionList);
			String scriptFunctionCountJson = objectMapper.writeValueAsString(scriptFunctionCountList);
			String customEnumerationJson = objectMapper.writeValueAsString(customEnumerationList);
			resp.setContentType("application/json");
			resp.getWriter()
					.write("{\"customDetailsList\":" + customDetailsJson + ",\"scriptConditionList\":"
							+ scriptConditionJson + ",\"scriptFunctionList\":" + scriptFunctionJson
							+ ",\"customEnumerationList\":" + customEnumerationJson + ",\"scriptCountList\":"
							+ scriptCountJson + ",\"scriptFunctionCountList\":" + scriptFunctionCountJson + "}");
		} catch (Exception e) {
			System.out.println("Error Message in customization Details is" + e.getMessage());
			e.printStackTrace();
		}
	}

	// Get custom Field Count with Respective WorkItem Type
	public void getCustomFieldDetails(ITrackerProject pro, ITypeOpt wiTypeEnum) throws UnresolvableObjectException {
		try {
			ICustomFieldsService customFieldService = trackerService.getDataService().getCustomFieldsService();
			Collection<ICustomField> customFieldList = customFieldService.getCustomFields("WorkItem",
					pro.getContextId(), wiTypeEnum.getId());
			int customFieldsCount = customFieldList.size();
			customDetailsList.add(new CustomDetailsPojo(customFieldsCount, wiTypeEnum));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Get customScriptCondition Count with Respective WorkItem Type
	public void getCustomWorkFlowScript(ITrackerProject pro, ITypeOpt wiTypeEnum) throws UnresolvableObjectException {

		try {
			int scriptCount = 0;
			IWorkflowConfig workFlow = trackerService.getWorkflowManager().getWorkflowConfig("WorkItem",
					wiTypeEnum.getId(), pro.getContextId());
			Collection<IAction> actions = workFlow.getActions();
			getCustomWorkFlowScriptFunction(actions, wiTypeEnum);
			for (IAction action : actions) {
				Set<IOperation> conditions = action.getConditions();
				for (IOperation conditionOperation : conditions) {
					if (conditionOperation.getName().equals("ScriptCondition")) {
						Map<String, String> conditionData = conditionOperation.getParams();
						for (Map.Entry<String, String> entry : conditionData.entrySet()) {
							if (entry.getKey().equalsIgnoreCase("script")) {
								scriptCount++;
								scriptConditionList.add(new ScriptConditionPojo(action, wiTypeEnum, entry.getValue()));
							}
						}
					}

				}
			}
			if (scriptCount > 0) {
				scriptCountList.add(new ScriptCountPojo(scriptCount, wiTypeEnum));
			}

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	// Get CustomScript FunctionCount with Respective WorkItmType
	public void getCustomWorkFlowScriptFunction(Collection<IAction> actions, ITypeOpt wiTypeEnum)
			throws UnresolvableObjectException {
		try {
			int scriptFunctionCount = 0;
			for (IAction action : actions) {
				Set<IOperation> functions = action.getFunctions();
				for (IOperation functionOperation : functions) {
					if (functionOperation.getName().equals("ScriptFunction")) {
						for (Map.Entry<String, String> entry : functionOperation.getParams().entrySet()) {
							if (entry.getKey().equalsIgnoreCase("script")) {
								scriptFunctionCount++;
								scriptFunctionList.add(new ScriptFunctionPojo(action, wiTypeEnum, entry.getValue()));
							}
						}

					}
				}
			}
			if (scriptFunctionCount > 0) {
				scriptFunctionCountList.add(new ScriptCountPojo(scriptFunctionCount, wiTypeEnum));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Get customEnumerationCount with Respective WorkItem Type
	public void getCustomEnumerationDetails(ITrackerProject pro, ITypeOpt wiTypeEnum)
			throws UnresolvableObjectException {
		try {
			Map<String, IEnumObjectFactory> enumerationObjectFactory = trackerService.getDataService()
					.getEnumerationObjectFactories();
			for (Map.Entry<String, IEnumObjectFactory> entry : enumerationObjectFactory.entrySet()) {
				String key = entry.getKey();
				IEnumObjectFactory value = entry.getValue();
				//System.out.println("The enumeration Object Factory" + key + ": " + value +"\n");
			}

		} catch (Exception e) {

		}
	}

	private void getPrePostSaveScript(Map<String, Object> responseData) {
		try {
			if (!(hookMap.isEmpty())) {
				hookMap.clear();
			}
			String folderPath = System.getProperty("com.polarion.home") + "/../scripts/" + "/workitemsave/";
			File folder = new File(folderPath);
			if (folder.exists() && folder.isDirectory()) {
				for (File jsFile : folder.listFiles()) {
					hookMap.put(jsFile.getName(), folderPath);
				}
			} else {
				System.out.println("The specified folder does not exist or is not a directory.");
			}

			responseData.put("hookMap", hookMap);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
		@SuppressWarnings("unchecked")
		public void getSpaceDetails(HttpServletRequest req, HttpServletResponse resp) throws IOException
		{
			String projectId =req.getParameter("projectId");
			JSONObject jsonObject = new JSONObject();
			ArrayList<JSONObject> data = new ArrayList<JSONObject>();
			 PrintWriter out = resp.getWriter();
			 
			 List<IFolder> spaces=trackerService.getFolderManager().getFolders(projectId);
			for(IFolder space : spaces)
			{
				JSONObject dataObj = new JSONObject();	
				
			Collection<IRichPage> liveReports=trackerService.getRichPageManager().getRichPages().project(projectId).space(space.getName());
			ArrayList<JSONObject> arrList=new ArrayList<JSONObject>();
			int count=0;
			for(IRichPage reports:liveReports) 
			{
					  String reportId=reports.getPageName();
					  if(!reportId.equals("Home"))
					  {
						  count++;
						  JSONObject reportDetails=new JSONObject();
						  Date created=reports.getCreated();
						  Date updatedDate=reports.getUpdated();
					      String pattern = "dd-MM-yyyy";
					      SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
						  String CreatedDate = simpleDateFormat.format(created);
						  String UpdatedDates=simpleDateFormat.format(updatedDate);
						  reportDetails.put("createdDates",CreatedDate.toString()); 
						  reportDetails.put("updatedDate", UpdatedDates.toString());
						  reportDetails.put("reportName", reports.getTitle());
						  arrList.add(reportDetails);
			}
			}
			if(count>0)
			{
		    dataObj.put("spaceName",space.getName());
			dataObj.put("reportDetails",arrList);
			data.add(dataObj);
			}
			
			}
			jsonObject.put("data", data);			
			out.println(jsonObject); 
		}
		
		@SuppressWarnings("unchecked")
		public void getPluginDetails(HttpServletRequest req, HttpServletResponse resp) throws IOException
		{
			PrintWriter out = resp.getWriter();
			ArrayList<JSONObject> data = new ArrayList<JSONObject>();
			JSONObject jsonObject = new JSONObject();
			String path=System.getProperty("com.polarion.home") + "/extensions/";
			 File directory = new File(path);
		        if (directory.exists() && directory.isDirectory()) {
		            File[] filesAndDirectories = directory.listFiles();
		            for (File fileOrDirectory : filesAndDirectories) {
		            	 if (fileOrDirectory.isDirectory()) {
		            	   JSONObject pluginDetails=new JSONObject();
		            		pluginDetails.put("pluginDeatils", fileOrDirectory.getName());
		            		pluginDetails.put("location", fileOrDirectory.getAbsoluteFile().toString());
		            		data.add(pluginDetails);
		            	 }
		            }
		            jsonObject.put("data", data);
					out.println(jsonObject);  
		        }
            }

		@SuppressWarnings("unchecked")
		public void getpolarionLicenseDetails(HttpServletRequest req, HttpServletResponse resp) throws IOException {
			ArrayList<JSONObject> licDetails =new ArrayList<JSONObject>();
			PrintWriter out = resp.getWriter();
			JSONObject jsonObject = new JSONObject();
		    String path = System.getProperty("com.polarion.home") + "/license/";
		    String filePath = path + "/polarion.lic";
		    try {
		    	
		        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		        DocumentBuilder builder = factory.newDocumentBuilder();
		        Document doc = builder.parse(filePath);
		        Element root = doc.getDocumentElement();
		        NodeList userNameList = root.getElementsByTagName("userName");
		        NodeList licenseTypeList =root.getElementsByTagName("licenseType");
		        NodeList userCompanyList =root.getElementsByTagName("userCompany");
		        JSONObject licenseDetails=new JSONObject();
		        if (userNameList.getLength() > 0) {
		            String userName = userNameList.item(0).getTextContent();
		            licenseDetails.put("userName", userName);
		        } 
		        if (licenseTypeList.getLength() > 0) {
		            String licenseType = licenseTypeList.item(0).getTextContent();
		            licenseDetails.put("licenseType", licenseType);
		        } 
		        if (userCompanyList.getLength() > 0) {
		            String userCompany = userCompanyList.item(0).getTextContent();
		            licenseDetails.put("userCompany", userCompany);
		        }
		        licDetails.add(licenseDetails);
		        
		    } catch (IOException e) {
		        e.printStackTrace();
		    } catch (Exception e) {
		        e.printStackTrace();
		    }
		    jsonObject.put("data", licDetails);
			out.println(jsonObject);
		}

}