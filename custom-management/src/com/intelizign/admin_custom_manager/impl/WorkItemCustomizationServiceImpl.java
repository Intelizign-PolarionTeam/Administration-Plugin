package com.intelizign.admin_custom_manager.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelizign.admin_custom_manager.service.ModuleCustomizationService;
import com.intelizign.admin_custom_manager.service.WorkItemCustomizationService;
import com.polarion.alm.projects.model.IFolder;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IRichPage;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.workflow.config.IAction;
import com.polarion.alm.tracker.workflow.config.IWorkflowConfig;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.IPlatformService;
import com.polarion.platform.ITransactionService;
import com.polarion.platform.persistence.ICustomFieldsService;
import com.polarion.platform.persistence.IEnumeration;
import com.polarion.platform.persistence.UnresolvableObjectException;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.service.repository.IRepositoryReadOnlyConnection;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.subterra.base.data.model.ICustomField;
import com.polarion.subterra.base.data.model.IPrimitiveType;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;

public class WorkItemCustomizationServiceImpl implements WorkItemCustomizationService {

	private static final Logger log = Logger.getLogger(WorkItemCustomizationServiceImpl.class);
	private static final String WORKFLOW_FUNCTION_KEY = "ScriptFunction";
	private static final String WORKFLOW_CONDITION_KEY = "ScriptCondition";
	private static final String WORKITEM_PROTOTYPE = "WorkItem";
	private static final String SCRIPT_PARAMETER = "script";
	private static final String DEFAULT_REPO = "default";
	private static final String WORKITEM_TYPE = "type";
	private static final String FUNCTION_KEY = "function";

	private ITrackerService trackerService;
	private ITransactionService transactionService;
	private IPlatformService platformService;
	private IRepositoryService repositoryService;
	private ModuleCustomizationService moduleCustomizationService;

	public Map<Integer, Map<String, Object>> customizationDetailsResponseData = new LinkedHashMap<>();
	public Map<Integer, Map<String, Object>> liveReportDetailsResponseMap = new LinkedHashMap<>();
	public Map<Integer, Map<String, Object>> pluginDetailsMap = new LinkedHashMap<>();
	public Map<Integer, Map<String, Object>> prePostSaveScriptMap = new LinkedHashMap<>();
	public Map<Integer, Map<String, Object>> licenseDetailsMap = new LinkedHashMap<>();
	private final ObjectMapper objectMapper = new ObjectMapper();

	private int wiWorkflowScriptConditionCount, wiWorkflowScriptFunctionCount, customEnumerationCount,
			wiCustomFieldCount;

	public WorkItemCustomizationServiceImpl(ITrackerService trackerService, ITransactionService transactionService,
			IRepositoryService repositoryService, IPlatformService platformService, ModuleCustomizationService moduleCustomizationService) {
		super();
		this.trackerService = trackerService;
		this.transactionService = transactionService;
		this.repositoryService = repositoryService;
		this.platformService = platformService;
		this.moduleCustomizationService = moduleCustomizationService;
	}

	/**
	 * Retrieves a list of projects and sends them as a JSON response.
	 * 
	 * @param req  The HttpServletRequest object containing request parameters.
	 * @param resp The HttpServletResponse object used to send the response.
	 */
	@Override
	public void retrieveAndSendProjectListAsJSON(HttpServletRequest req, HttpServletResponse resp) {
		try {
			IPObjectList<IProject> getProjectList = trackerService.getProjectsService().searchProjects("", "id");

			Map<String, String> projectsObjMap = getProjectList.stream().map(pro -> {
				try {
					return Map.entry(pro.getId(), pro.getName());
				} catch (UnresolvableObjectException e) {
					log.error("Skipping entry due to UnresolvableObjectException: " + e.getMessage());
					return null;
				}
			}).filter(entry -> entry != null).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
					(existing, replacement) -> existing, LinkedHashMap::new));

			Map<String, Object> responseObject = new LinkedHashMap<>();
			responseObject.put("projectsList", projectsObjMap);

			ObjectMapper objectMapper = new ObjectMapper();
			String jsonResponse = objectMapper.writeValueAsString(responseObject);

			resp.setContentType("application/json");
			resp.getWriter().write(jsonResponse);
		} catch (Exception e) {
			log.error("Error occurred while fetching project list: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Fetches and organizes customization count details for work item types and modules, 
	 * and sends them as a JSON response.
	 * @param req  The HttpServletRequest object containing request parameters.
	 * @param resp The HttpServletResponse object used to send the response.
	 * @throws Exception If an error occurs during the process.
	 */
	@Override
	public void fetchAndOrganizeCustomizationCountDetails(HttpServletRequest req, HttpServletResponse resp) 
			throws Exception {
		try {
			String projectId = req.getParameter("projectId");
		    String productId = platformService.getPolarionProductName();
		    String versionId= platformService.getPolarionVersion();
		    
		   
			ITrackerProject projectObject = trackerService.getTrackerProject(projectId);
			List<ITypeOpt> workItemTypeEnum = trackerService.getTrackerProject(projectId).getWorkItemTypeEnum()
					.getAvailableOptions(WORKITEM_TYPE);

			Stream.of(liveReportDetailsResponseMap, pluginDetailsMap, prePostSaveScriptMap, licenseDetailsMap)
					.filter(Objects::nonNull).forEach(Map::clear);

			List<Map<String, Object>> customizationCountDetailsList = workItemTypeEnum.stream().map(wiTypeEnum -> {
				storeAllWorkItemCustomizationCount(projectObject, wiTypeEnum);

				Map<String, Object> customizationCountDetailsMap = new LinkedHashMap<>();
				customizationCountDetailsMap.put("wiType", wiTypeEnum.getId());
				customizationCountDetailsMap.put("wiName", wiTypeEnum.getName());
				customizationCountDetailsMap.put("wiWorkflowScriptConditionCount", wiWorkflowScriptConditionCount);
				customizationCountDetailsMap.put("wiWorkflowScriptFunctionCount", wiWorkflowScriptFunctionCount);
				customizationCountDetailsMap.put("customEnumerationCount", customEnumerationCount);
				customizationCountDetailsMap.put("wiCustomFieldCount", wiCustomFieldCount);

				return customizationCountDetailsMap;
			}).collect(Collectors.toList());

			List<Map<String, Object>> moduleCustomizationCountDetailsList = moduleCustomizationService
					.getModuleCustomizationCountDetails(req, resp);
			
			addLiveReportDetailsInMapObject(req, resp);
			addPluginDetailsInMapObject(req, resp);
			addPrePostSaveScriptDetailsInMapObject(req, resp);
			addLicenseDetailsInMapObject(req, resp);

			Map<String, Object> jsonResponse = new LinkedHashMap<>();
			jsonResponse.put("customizationCountDetails", customizationCountDetailsList);
			jsonResponse.put("moduleCustomizationDetails", moduleCustomizationCountDetailsList);
			jsonResponse.put("liveReportDetailsResponse", liveReportDetailsResponseMap);
			jsonResponse.put("pluginDetails", pluginDetailsMap);
			jsonResponse.put("prePostSaveScriptDetails", prePostSaveScriptMap);
			jsonResponse.put("licenseDetails", licenseDetailsMap);
			jsonResponse.put("productId", productId);
			jsonResponse.put("versionId", versionId);

			String jsonResponseString = objectMapper.writeValueAsString(jsonResponse);

			resp.setContentType("application/json");
			resp.getWriter().write(jsonResponseString);

		} catch (Exception e) {
			System.out.println("Error Message in customization Details is" + e.getMessage());
			e.printStackTrace();
		}
	}
	/**
	 * Retrieves License Details and add to the licenseDetailsMap Object 
	 * Attributes - pluginDetails, pluginPath
	 */
	private void addLicenseDetailsInMapObject(HttpServletRequest req, HttpServletResponse resp)
	throws Exception{
		try {

			String folderPath = System.getProperty("com.polarion.home") + "/../polarion/license/";
			File folder = new File(folderPath);
			if (folder.exists() && folder.isDirectory()) {
				File licFile = new File(folder, "polarion.lic");
				if (licFile.exists() && licFile.isFile()) {
					AtomicInteger id = new AtomicInteger(0);
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					NodeList nodeList = factory.newDocumentBuilder().parse(licFile).getDocumentElement()
							.getChildNodes();
					for (int i = 0; i < nodeList.getLength(); i++) {
						Node node = nodeList.item(i);
						if (node.getNodeType() == Node.ELEMENT_NODE) {
							String nodeName = node.getNodeName();
							String nodeValue = node.getTextContent().trim();
							licenseDetailsMap.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
							switch (nodeName) {
							case "licenseType":
								licenseDetailsMap.get(id.get()).put("licenseType", nodeValue);
								id.getAndIncrement();
								break;
							case "userCompany":
								licenseDetailsMap.get(id.get()).put("userCompany", nodeValue);
								id.getAndIncrement();
								break;
							case "userEmail":
								licenseDetailsMap.get(id.get()).put("userEmail", nodeValue);
								id.getAndIncrement();
								break;
							case "userName":
								licenseDetailsMap.get(id.get()).put("userName", nodeValue);
								id.getAndIncrement();
								break;

							}

						}
					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * Retrieves plugin Details and add to the pluginDetailsMap Object 
	 * Attributes - pluginDetails, pluginPath
	 */
	private void addPluginDetailsInMapObject(HttpServletRequest req, HttpServletResponse resp) 
			throws Exception {
		String path = System.getProperty("com.polarion.home") + "/extensions/";
		File directory = new File(path);
		AtomicInteger id = new AtomicInteger(0);
		if (directory.exists() && directory.isDirectory()) {
			Arrays.stream(directory.listFiles()).forEach(dir -> {
				pluginDetailsMap.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
				pluginDetailsMap.get(id.get()).put("pluginDetails", dir.getName());
				pluginDetailsMap.get(id.get()).put("pluginPath", dir.getAbsolutePath());
				id.getAndIncrement();
			});
		} else {
			log.error("The Specified directory not exists");
		}

	}
	
	//Extract FunctionName in PrePost hook script
	private String extractFunctionNames(String content) 
			throws Exception {
		Pattern pattern = Pattern.compile("function\\s+([\\w\\d_]+)\\s*\\(");
		Matcher matcher = pattern.matcher(content);
		List<String> functionNames = new ArrayList<>();
		while (matcher.find()) {
			functionNames.add(matcher.group(1));
		}
		return functionNames.isEmpty() ? "No functions found in the file."
				: functionNames.stream().collect(Collectors.joining(", "));
	}
	
	//Read prepost hook file
	private String readFileContent(File file)
	throws Exception{
		try (Stream<String> lines = Files.lines(file.toPath())) {
			return lines.collect(Collectors.joining("\n"));
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Retrieves prepost save Details and add to the prePostSaveScriptMap Object 
	 * Attributes - Name, Extension
	 */
	private void addPrePostSaveScriptDetailsInMapObject(HttpServletRequest req, HttpServletResponse resp)
	throws Exception{
		String folderPath = System.getProperty("com.polarion.home") + "/../scripts/" + "/workitemsave/";
		File folder = new File(folderPath);
		try {
			if (folder.exists() && folder.isDirectory()) {
				AtomicInteger id = new AtomicInteger(0);
				Arrays.stream(folder.listFiles()).forEach(jsFile -> {
					try {
						String content = readFileContent(jsFile);
						Map<String, Object> scriptData = new HashMap<>();
						scriptData.put("jsName", jsFile.getName());
						String functionNames = content.contains(FUNCTION_KEY) ? extractFunctionNames(content)
								: "No functions found in the file.";
						prePostSaveScriptMap.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
						prePostSaveScriptMap.get(id.get()).put("Name", jsFile.getName());
						prePostSaveScriptMap.get(id.get()).put("Extension", functionNames);
						id.getAndIncrement();
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			} else {
				log.error("The specified folder does not exist or is not a directory.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Retrieves Live Report Details and add to the liveReportDetailsResponseMap Object 
	 * Attributes - folderName, createdDate, updatedDate, Report Name
	 */
	private void addLiveReportDetailsInMapObject(HttpServletRequest req, HttpServletResponse resp) throws Exception {
		String projectId = req.getParameter("projectId");
		List<IFolder> spaces = trackerService.getFolderManager().getFolders(projectId);
		AtomicInteger id = new AtomicInteger(0);

		spaces.forEach(space -> {
			Collection<IRichPage> liveReportsObj = trackerService.getRichPageManager().getRichPages().project(projectId)
					.space(space.getName());
			liveReportsObj.stream().filter(report -> !report.getPageName().equals("Home")).forEach(report -> {
				Date created = report.getCreated();
				Date updatedDate = report.getUpdated();
				String pattern = "dd-MM-yyyy";
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
				String CreatedDate = simpleDateFormat.format(created);
				String UpdatedDate = simpleDateFormat.format(updatedDate);
				liveReportDetailsResponseMap.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
				liveReportDetailsResponseMap.get(id.get()).put("folderName", space.getName());
				liveReportDetailsResponseMap.get(id.get()).put("createdDate", CreatedDate.toString());
				liveReportDetailsResponseMap.get(id.get()).put("updatedDate", UpdatedDate.toString());
				liveReportDetailsResponseMap.get(id.get()).put("reportName", report.getTitle());
				liveReportDetailsResponseMap.get(id.get()).put("reportId", report.getId());
				id.getAndIncrement();
			});
		});
	}

	/**
	 * Retrieves all customization count related to workItem
	 * 
	 * @param pro        -The tracker project object
	 * @param wiTypeEnum - The workItem enum object
	 */
	private void storeAllWorkItemCustomizationCount(ITrackerProject trackerPro, ITypeOpt wiTypeEnum) {
		try {
			storeWorkItemCustomFieldCount(trackerPro, wiTypeEnum);
			storeWorkItemWorkFlowSciptCount(trackerPro, wiTypeEnum);
			storeWorkItemCustomEnumerationCount(trackerPro, wiTypeEnum);
		} catch (UnresolvableObjectException e) {
			log.error("Skipping entry due to UnresolvableObjectException: " + e.getMessage());
		} catch (Exception e) {
			log.error("Exception is" + e.getMessage());
		}
	}

	/**
	 * Retrieves customField count with respective workItem type stored in
	 * wiCustomFieldCount variable
	 * 
	 * @param pro        -The tracker project object
	 * @param wiTypeEnum - The workItem enum object
	 */
	private void storeWorkItemCustomFieldCount(ITrackerProject pro, ITypeOpt wiTypeEnum) {
		try {
			ICustomFieldsService customFieldService = trackerService.getDataService().getCustomFieldsService();
			Collection<ICustomField> customFieldList = customFieldService.getCustomFields(WORKITEM_PROTOTYPE,
					pro.getContextId(), wiTypeEnum.getId());
			wiCustomFieldCount = customFieldList.size();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Retrieves workflow condition count with respective workItem type stored in
	 * wiWorkflowScriptConditionCount variable
	 * 
	 * @param pro        -The tracker project object
	 * @param wiTypeEnum - The workItem enum object
	 */
	private void storeWorkItemWorkFlowSciptCount(ITrackerProject pro, ITypeOpt wiTypeEnum) {
		try {
			IWorkflowConfig workFlow = trackerService.getWorkflowManager().getWorkflowConfig(
					WorkItemCustomizationServiceImpl.WORKITEM_PROTOTYPE, wiTypeEnum.getId(), pro.getContextId());
			Collection<IAction> actions = workFlow.getActions();
			storeWorkItemWorkFlowFunctionCount(actions, wiTypeEnum);
			wiWorkflowScriptConditionCount = (int) actions.stream().flatMap(action -> action.getConditions().stream())
					.filter(condition -> condition.getName().equals(WORKFLOW_CONDITION_KEY))
					.flatMap(condition -> condition.getParams().entrySet().stream())
					.filter(entry -> entry.getKey().equalsIgnoreCase(SCRIPT_PARAMETER)).count();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Retrieves workflow function count with respective workItem type stored in
	 * wiWorkflowScriptFunctionCount variable
	 * 
	 * @param actions    -collection of IAction object related to workitem workflow
	 * @param wiTypeEnum - The workItem enum object
	 */
	private void storeWorkItemWorkFlowFunctionCount(Collection<IAction> actions, ITypeOpt wiTypeEnum) {
		try {
			wiWorkflowScriptFunctionCount = (int) actions.stream().flatMap(action -> action.getFunctions().stream())
					.filter(function -> function.getName().equals(WORKFLOW_FUNCTION_KEY)).count();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Retrieves customEnumeration count in with respective workItem type stored in
	 * customEnumerationCount variable
	 * 
	 * @param pro-The    tracker project object
	 * @param wiTypeEnum - The workItem enum object
	 * @throws Exception If an error occurs during the customization processing.
	 */
	private void storeWorkItemCustomEnumerationCount(ITrackerProject pro, ITypeOpt wiTypeEnum) throws Exception {
		try {
			String projectLocation = pro.getLocation().getLocationPath();
			IEnumeration<ITypeOpt> wiType = trackerService.getTrackerProject(pro).getWorkItemTypeEnum();
			List<String> typeIds = wiType.getAllOptions().stream().map(ITypeOpt::getId).collect(Collectors.toList());
			transactionService.beginTx();
			ILocation config = Location.getLocationWithRepository(DEFAULT_REPO,
					projectLocation+ "/.polarion/tracker/fields/");
			IRepositoryReadOnlyConnection defaultRepo = repositoryService.getReadOnlyConnection(DEFAULT_REPO);
			InputStream inputStrm = defaultRepo.getContent(config);
			InputStreamReader inputStreamReader = new InputStreamReader(inputStrm);
			BufferedReader reader = new BufferedReader(inputStreamReader);
			try {
				Pattern pattern = Pattern.compile("<a href=\"(.*?)\">(.*?)</a>");
				long count = reader.lines().flatMap(line -> {
					java.util.regex.Matcher matcher = pattern.matcher(line);
					List<String> matches = new ArrayList<>();
					while (matcher.find()) {
						String extractedText = matcher.group(2);
						if (typeIds.stream().anyMatch(prefix -> extractedText.startsWith(prefix))
								&& !extractedText.contains("custom-fields")
								&& !extractedText.contains("calculated-fields")) {
							String typeIdPrefix = extractedText.substring(0, extractedText.indexOf('-'));
							if (typeIdPrefix.equals(wiTypeEnum.getId())) {
								matches.add(extractedText);
							}
						}
					}
					return matches.stream();
				}).count();
				customEnumerationCount = (int) count;

			} finally {
				try {
					reader.close();
					inputStrm.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			System.out.println("Exception is" + e.getMessage());
		} finally {
			transactionService.endTx(false);
		}
	}

	/**
	 * Handles FetchAndSendCustomizationDetails API
	 * 
	 * @param resp -wrap the responses in jsonResponse Object
	 * @param req  -getting request from frontend
	 */
	@Override
	public void FetchAndSendCustomizationDetails(HttpServletRequest req, HttpServletResponse resp) throws Exception {
		try {
			String type = req.getParameter("type");
			String heading = req.getParameter("heading");
			String projectId = req.getParameter("projectId");

			if (!customizationDetailsResponseData.isEmpty()) {
				customizationDetailsResponseData.clear();
			}
			if (type != null && heading != null && projectId != null) {
				ITrackerProject trackerPro = trackerService.getTrackerProject(projectId);
				List<ITypeOpt> moduleTypeEnum = trackerPro.getModuleTypeEnum().getAvailableOptions(WORKITEM_TYPE);
				List<ITypeOpt> workItemTypeEnum = trackerPro.getWorkItemTypeEnum().getAvailableOptions(WORKITEM_TYPE);
				workItemTypeEnum.stream().filter(wiType -> wiType.getId().equalsIgnoreCase(type)).forEach(wiType -> {
					try {
						redirectWorkItemCustomization(heading, wiType, trackerPro);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});

				moduleTypeEnum.stream().filter(moduleType -> moduleType.getId().equalsIgnoreCase(type))
						.forEach(moduleType -> {
							try {
								customizationDetailsResponseData = moduleCustomizationService
										.getModuleCustomizationDetails(trackerPro, moduleType, heading);
							} catch (Exception e) {
								e.printStackTrace();
							}
						});

			} else {
				log.error("Passing Data is Not Acceptable");
			}

			Map<String, Object> jsonResponse = new LinkedHashMap<>();
			jsonResponse.put("customizationDetailsResponseData", customizationDetailsResponseData);

			String customizationDetailsResponseJson = objectMapper.writeValueAsString(jsonResponse);

			resp.setContentType("application/json");
			resp.getWriter().write(customizationDetailsResponseJson);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Redirects to the appropriate method based on the heading provided. From
	 * Frontend when user trigger the count respective heading passed
	 *
	 * @param heading      The heading indicating the type of customization.
	 * @param workItemType The work item type.
	 * @param trackerPro   The tracker project.
	 * @throws Exception If an error occurs during the customization processing.
	 */
	private void redirectWorkItemCustomization(String heading, ITypeOpt workItemType, ITrackerProject trackerPro)
			throws Exception {
		switch (heading) {
		case "wiCustomFieldCount":
			addWorkItemCustomFieldDetailsInMapObject(workItemType, trackerPro);
			break;
		case "customEnumerationCount":
			addWorkItemCustomEnumerationDetailsInMapObject(workItemType, trackerPro);
			break;
		case "wiWorkflowScriptFunctionCount":
			addWorkItemWorkFlowFunctionDetailsInMapObject(workItemType, trackerPro);
			break;
		case "wiWorkflowScriptConditionCount":
			addWorkItemWorkFlowConditionDetailsInMapObject(workItemType, trackerPro);
			break;
		default:
			break;
		}
	}

	/**
	 * Retrieves and maps workItem custom Enumeration details associated with a work
	 * item type and project. Attributes customEnumeration
	 * 
	 * @param wiType  The workItem Type
	 * @param project The Tracker Project
	 * @throws Exception If an error occurs during the customization processing.
	 */
	private void addWorkItemCustomEnumerationDetailsInMapObject(ITypeOpt wiType, ITrackerProject project)
			throws Exception {
		try {
			String projectLocation = project.getLocation().getLocationPath();
			IEnumeration<ITypeOpt> wiEnumObj = project.getWorkItemTypeEnum();
			List<String> wiTypeList = wiEnumObj.getAllOptions().stream().map(ITypeOpt::getId)
					.collect(Collectors.toList());
			transactionService.beginTx();
			ILocation config = Location.getLocationWithRepository(DEFAULT_REPO,
					projectLocation + "/.polarion/tracker/fields/");
			IRepositoryReadOnlyConnection defaultRepo = repositoryService.getReadOnlyConnection(DEFAULT_REPO);
			InputStream inputStrm = defaultRepo.getContent(config);
			InputStreamReader inputStreamReader = new InputStreamReader(inputStrm);
			BufferedReader readEnumerationXml = new BufferedReader(inputStreamReader);
			try {
				processEnumeration(readEnumerationXml, wiTypeList, wiType);
			} finally {
				closeResources(readEnumerationXml, inputStrm);
			}
		} catch (Exception e) {
			System.out.println("Exception occurred: " + e.getMessage());
		} finally {
			try {
				transactionService.endTx(false);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Closed the Input Stream For Enumeration.xml File
	 * 
	 * @param reader
	 * @param inputStrm
	 */
	private void closeResources(BufferedReader reader, InputStream inputStrm) {
		try {
			if (reader != null)
				reader.close();
			if (inputStrm != null)
				inputStrm.close();
		} catch (Exception e) {
			System.out.println("Error closing resources: " + e.getMessage());
		}
	}

	/**
	 * Extracting Text From Respective Enumeration.xml File and add to the Map
	 * Object Attribute customEnumeration
	 * 
	 * @param readEnumerationXml
	 * @param wiTypeList         -List of workitem type
	 * @param wiType             - specific workitem
	 * @throws IOException if an error occurs during customization
	 */
	private void processEnumeration(BufferedReader readEnumerationXml, List<String> wiTypeList, ITypeOpt wiType)
			throws Exception {
		AtomicInteger id = new AtomicInteger(0);
		Pattern pattern = Pattern.compile("<a href=\"(.*?)\">(.*?)</a>");
		readEnumerationXml.lines().flatMap(line -> {
			Matcher matcher = pattern.matcher(line);
			List<String> matches = new ArrayList<>();
			while (matcher.find()) {
				String extractedText = matcher.group(2);
				if (wiTypeList.stream().anyMatch(prefix -> extractedText.startsWith(prefix))
						&& !extractedText.contains("custom-fields") && !extractedText.contains("calculated-fields")) {
					String wiTypePrefix = extractedText.substring(0, extractedText.indexOf('-'));
					if (wiTypePrefix.equals(wiType.getId())) {
						matches.add(extractedText);
					}
				}
			}
			return matches.stream();
		}).forEach(extractedText -> {
			customizationDetailsResponseData.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
			customizationDetailsResponseData.get(id.get()).put("customEnumeration", extractedText);
			id.getAndIncrement();
		});
	}

	/**
	 * Retrieves and maps workItem workflow condition details associated with a work
	 * item type and project. Attributes actionId, actionName, attachedJsFile
	 * 
	 * @param wiType  -The workItem type
	 * @param project The tracker project object
	 */
	private void addWorkItemWorkFlowConditionDetailsInMapObject(ITypeOpt wiType, ITrackerProject project) 
	throws Exception{
		AtomicInteger id = new AtomicInteger(0);
		try {
			IWorkflowConfig workFlow = trackerService.getWorkflowManager().getWorkflowConfig(WORKITEM_PROTOTYPE,
					wiType.getId(), project.getContextId());
			workFlow.getActions().forEach(action -> {
				action.getConditions().forEach(conditionsOperation -> {
					if (conditionsOperation.getName().equals(WORKFLOW_CONDITION_KEY)) {
						conditionsOperation.getParams().forEach((key, value) -> {
							if (key.equalsIgnoreCase(SCRIPT_PARAMETER)) {
								customizationDetailsResponseData.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
								customizationDetailsResponseData.get(id.get()).put("actionId", action.getId());
								customizationDetailsResponseData.get(id.get()).put("actionName", action.getName());
								customizationDetailsResponseData.get(id.get()).put("attachedJsFile", value);
								id.getAndIncrement();
							}
						});
					}
				});
			});
		} catch (Exception e) {
			log.error("Error while Fetching Data in WorkItemWorkflow Condition" + e.getMessage());
		}
	}

	/**
	 * Retrieves and maps workItem custom Field details associated with a work item
	 * type and project. Attributes customId, customName
	 * 
	 * @param wiType    -The workItem type
	 * @param projectId -The tracker project object
	 */
	private void addWorkItemCustomFieldDetailsInMapObject(ITypeOpt wiType, ITrackerProject projectId)
	throws Exception{
		AtomicInteger id = new AtomicInteger(0);
		ICustomFieldsService customFieldService = trackerService.getDataService().getCustomFieldsService();
		Collection<ICustomField> customFieldList = customFieldService.getCustomFields(WORKITEM_PROTOTYPE,
				projectId.getContextId(), wiType.getId());
		customFieldList.forEach(cust -> {
			customizationDetailsResponseData.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
			customizationDetailsResponseData.get(id.get()).put("customId", cust.getId());
			customizationDetailsResponseData.get(id.get()).put("customName", cust.getName());
			if (cust.getType() instanceof IPrimitiveType) {
				IPrimitiveType modulePrimitiveType = (IPrimitiveType) cust.getType();
				customizationDetailsResponseData.get(id.get()).put("customType", modulePrimitiveType.getTypeName());
			}
			id.getAndIncrement();
		});
	}

	/**
	 * Retrieves and maps workItem workflow function details associated with a work
	 * item type and project. Attributes actionId, actionName, attachedJsFile
	 * 
	 * @param wiType  -The workItem type
	 * @param project -The tracker project object
	 */
	private void addWorkItemWorkFlowFunctionDetailsInMapObject(ITypeOpt wiType, ITrackerProject project) 
			throws Exception {
		AtomicInteger id = new AtomicInteger(0);
		IWorkflowConfig workFlow = trackerService.getWorkflowManager().getWorkflowConfig(WORKITEM_PROTOTYPE,
				wiType.getId(), project.getContextId());
		workFlow.getActions().forEach(action -> {
			action.getFunctions().forEach(functionOperation -> {
				if (functionOperation.getName().equals(WORKFLOW_FUNCTION_KEY)) {
					functionOperation.getParams().forEach((key, value) -> {
						if (key.equalsIgnoreCase(SCRIPT_PARAMETER)) {
							customizationDetailsResponseData.computeIfAbsent(id.get(), k -> new LinkedHashMap<>());
							customizationDetailsResponseData.get(id.get()).put("actionId", action.getId());
							customizationDetailsResponseData.get(id.get()).put("actionName", action.getName());
							customizationDetailsResponseData.get(id.get()).put("attachedJsFile", value);
							id.getAndIncrement();
						}
					});
				}
			});
		});
	}

}