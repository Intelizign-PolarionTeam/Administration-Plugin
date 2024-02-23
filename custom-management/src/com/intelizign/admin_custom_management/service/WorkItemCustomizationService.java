package com.intelizign.admin_custom_management.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.polarion.platform.persistence.UnresolvableObjectException;

public interface WorkItemCustomizationService {

	void getProjectList(HttpServletRequest req, HttpServletResponse resp) throws UnresolvableObjectException;

	void getCustomizationCountDetails(HttpServletRequest req, HttpServletResponse resp) throws Exception;

	void getCustomizationDetails(HttpServletRequest req, HttpServletResponse resp) throws Exception;

}
