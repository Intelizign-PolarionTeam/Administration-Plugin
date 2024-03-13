package com.intelizign.admin_custom_data_explorer.servlet;

import com.intelizign.admin_custom_data_explorer.impl.ModuleCustomizationServiceImpl;
import com.intelizign.admin_custom_data_explorer.impl.WorkItemCustomizationServiceImpl;
import com.intelizign.admin_custom_data_explorer.service.ModuleCustomizationService;
import com.intelizign.admin_custom_data_explorer.service.WorkItemCustomizationService;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.platform.IPlatformService;
import com.polarion.platform.ITransactionService;
import com.polarion.platform.core.PlatformContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.polarion.core.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.IOException;



public class AdminCustomManagementServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(AdminCustomManagementServlet.class);
	private static final ITrackerService trackerService = (ITrackerService) PlatformContext.getPlatform()
			.lookupService(ITrackerService.class);
	private static final ITransactionService transactionService = (ITransactionService) PlatformContext.getPlatform()
			.lookupService(ITransactionService.class);
	private static final IRepositoryService repositoryService = (IRepositoryService) PlatformContext.getPlatform()
			.lookupService(IRepositoryService.class);
	private static final IPlatformService platformService = (IPlatformService) PlatformContext.getPlatform()
			.lookupService(IPlatformService.class);
	private ModuleCustomizationService moduleCustomizationService ;
	private WorkItemCustomizationService workItemCustomizationService ;
	

	
	@Override
	public void init() throws ServletException {
		super.init();
		this.moduleCustomizationService = new ModuleCustomizationServiceImpl(trackerService);
		this.workItemCustomizationService = new WorkItemCustomizationServiceImpl
				(trackerService,transactionService,repositoryService,platformService,moduleCustomizationService);
		
	}
	
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		String action = req.getParameter("action");
		try {
			if (action != null) {  
				switch (action) {
				case "getProjectList":
					workItemCustomizationService.retrieveAndSendProjectListAsJSON(req, resp);
					break;
				case "getCustomizationCountDetails":
					workItemCustomizationService.fetchAndOrganizeCustomizationCountDetails(req, resp);
					
					break;
				case "getCustomizationDetails":
					  workItemCustomizationService.FetchAndSendCustomizationDetails(req, resp);
					break;
				default:
					throw new IllegalArgumentException("Invalid action specified");
				}
			}
		} catch (Exception e) {
			log.error("Exception is" + e.getMessage());
		}
		if (action == null) {
			getServletContext().getRequestDispatcher("/static/index.html").forward(req, resp);
		}
	}

}