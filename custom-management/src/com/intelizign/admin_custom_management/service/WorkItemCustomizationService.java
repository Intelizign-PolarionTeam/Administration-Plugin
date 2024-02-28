package com.intelizign.admin_custom_management.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.polarion.platform.persistence.UnresolvableObjectException;

public interface WorkItemCustomizationService {

	void retrieveAndSendProjectListAsJSON(HttpServletRequest req, HttpServletResponse resp) throws UnresolvableObjectException;

	void fetchAndOrganizeCustomizationCountDetails(HttpServletRequest req, HttpServletResponse resp) throws Exception;

	void FetchAndSendCustomizationDetails(HttpServletRequest req, HttpServletResponse resp) throws Exception;
}
