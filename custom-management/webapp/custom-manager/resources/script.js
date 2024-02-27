$(document).ready(function() {
	$.ajax({
		url: 'custommanager?action=getProjectList',
		type: 'GET',
		dataType: 'json',
		success: function(response) {
			const projectsList = response.projectsList;
			//const prePostHookMapObj = response.prePostHookMapObj
			if (response) {
				var projectSelect = $('.form-control');
				projectSelect.empty();
				projectSelect.append($('<option>', { value: '', text: 'Select a project' }));
				$.each(projectsList, function(id, name) {
					projectSelect.append($('<option>', {
						value: id,
						text: name
					}));
				});
			} else {
				console.error('Failed to retrieve project list');
			}
		},
		error: function(error) {
			console.error('Error occurred while fetching project list:', error);
		}
	});
});

function projectInfo() {
	const projectId = $("#projectDropDown").val();
	alert("Selected project Id is: " + projectId);
	$('.polarion-rpw-table-content').show();
	$('#pre-post').css('display', 'block');
	$.ajax({
		url: `custommanager?action=getCustomizationCountDetails&projectId=${projectId}`,
		type: "GET",
		dataType: "json",
		success: function(data) {

			const wiCustomizationObj = data.customizationCountDetails;
			const moduleCustomizationObj = data.moduleCustomizationDetails;
			const liveReportDetailsObj = data.liveReportDetailsResponse;
			const prePostSaveScriptObj = data.prePostSaveScriptDetails;
			const licenseDetailsObj = data.licenseDetails;
			const pluginDetailsObj = data.pluginDetails;
			console.log("customizationDetails:", wiCustomizationObj);
			console.log("moduleCustomizationObj:", moduleCustomizationObj);
			console.log("liveReportDetailsResponse:", liveReportDetailsObj);
			console.log("prePostSaveScriptObj:", prePostSaveScriptObj);
			console.log("licenseDetails:", licenseDetailsObj);
			console.log("pluginDetails:", pluginDetailsObj);


			
		
			$('#userTableBody').empty();
			$('#userTableBodyDocument').empty();
	
			workItemCustomizationTable(wiCustomizationObj);
			moduleCustomizationTable(moduleCustomizationObj);
			liveReportCustomizationTable(liveReportDetailsObj);
		},
		error: function(error) {
			console.error("Error occurred:", error);
		}
	});
}

function workItemCustomizationTable(wiCustomizationObj) {
	$.each(wiCustomizationObj, function(index, wiCustom) {

		if (wiCustom.hasOwnProperty('wiType') && wiCustom.hasOwnProperty('wiName') &&
			wiCustom.hasOwnProperty('wiCustomFieldCount') && wiCustom.hasOwnProperty('wiWorkflowScriptConditionCount') &&
			wiCustom.hasOwnProperty('customEnumerationCount') && wiCustom.hasOwnProperty('wiWorkflowScriptFunctionCount')) {

		
			var row = $('<tr>').addClass('table-content-row');
			row.append($('<td>').text(wiCustom.wiName).css('text-align', 'center'));

			['wiCustomFieldCount', 'customEnumerationCount', 'wiWorkflowScriptConditionCount', 'wiWorkflowScriptFunctionCount'].forEach(function(countType) {
				var count = wiCustom[countType];
				var countCell = $('<td>').css('text-align', 'center');
				if (count > 0) {
					var hyperlink = $('<a>').addClass('data-span clickable-cell')
						.text(count)
						.data('heading', countType)
						.data('type',  wiCustom.wiType);
						
					countCell.append(hyperlink);
				} else {
					countCell.text(count);
				}
				row.append(countCell);
			});

			$('#userTableBody').append(row);
		}
	});
}

function moduleCustomizationTable(moduleCustomizationObj) {
	$.each(moduleCustomizationObj, function(index, moduleCustom) {
	

		if (moduleCustom.hasOwnProperty('moduleType') && moduleCustom.hasOwnProperty('moduleName') && moduleCustom.hasOwnProperty('moduleCustomfieldCount') &&
			moduleCustom.hasOwnProperty('moduleWorkflowFunctionCount') && moduleCustom.hasOwnProperty('moduleWorkflowConditionCount')) {

			var row = $('<tr>').addClass('table-content-row');
			row.append($('<td>').text(moduleCustom.moduleName).css('text-align', 'center'));

			['moduleCustomfieldCount', 'moduleWorkflowConditionCount', 'moduleWorkflowFunctionCount'].forEach(function(countType) {
				var count = moduleCustom[countType];
				var countCell = $('<td>').css('text-align', 'center');
				if (count > 0) {
					var hyperlink = $('<a>').addClass('data-span clickable-cell')
						.text(count)
						.data('heading', countType)
						.data('type', moduleCustom.moduleType);


					countCell.append(hyperlink);
				} else {
					countCell.text(count);
				}
				row.append(countCell);
			});

			$('#userTableBodyDocument').append(row);
		}
	});
}

function liveReportCustomizationTable(liveReportObj) {
 
    const uniqueFolderNames = {};

    $.each(liveReportObj, function(index, reportObj) {

        if (reportObj.hasOwnProperty('folderName') && reportObj.hasOwnProperty('createdDate') &&
            reportObj.hasOwnProperty('updatedDate') && reportObj.hasOwnProperty('reportName')) {



            if (!uniqueFolderNames[reportObj.folderName]) {
                var row = $('<tr>').addClass('table-content-row');
                row.append($('<td>').text(reportObj.folderName).css('text-align', 'center'));

                ['reportName', 'createdDate', 'updatedDate'].forEach(function(reportEvent) {
                    var eventValue = reportObj[reportEvent];
                    var countCell = $('<td>').css('text-align', 'center');
                    if (eventValue > 0) {
                        var hyperlink = $('<a href="' + getReportUrl(projectId, reportObj.folderName, reportName) + reportName + '</a>').addClass('data-span clickable-cell')
                            .text(eventValue);
 					countCell.append(hyperlink);
                    } else {
                        countCell.text(eventValue);
                    }
                    row.append(countCell);
                });

                $('#liveReportTableBody').append(row);

        
                uniqueFolderNames[reportObj.folderName] = true;
            }
        }
    });
}


function getReportUrl(projectId, spaceName, reportName) {
    var baseUrl = window.location.protocol + '//' + window.location.host;
    var polarionStartingUrl = '/polarion/#/project/';
    if (spaceName === '_default') {
        return baseUrl + polarionStartingUrl + projectId + '/wiki/' + reportName;
    } else {
        return baseUrl + polarionStartingUrl + projectId + '/wiki/' + spaceName + '/' + reportName;
    }
}




$(document).on('click', '.clickable-cell', function() {
    const projectId = $("#projectDropDown").val();
    alert("Selected project Id is: " + projectId);
    var heading = $(this).data('heading');
    var type = $(this).data('type');
    console.log("Heading: " + heading + "\nType: " + type);

    $.ajax({
        url: `custommanager?action=getCustomizationDetails&heading=${heading}&type=${type}&projectId=${projectId}`,
        method: 'GET',

        success: function(response) {
    
            const customizationDetailsResponseData = response.customizationDetailsResponseData;
            console.log("Heading after getting response"+heading);
            if(heading === "moduleCustomfieldCount"  || heading === "wiCustomFieldCount" ){
            showCustomFieldModelPopup(type , customizationDetailsResponseData);
            }else if(heading === "moduleWorkflowConditionCount"  || heading === "wiWorkflowScriptConditionCount"){
			showWorkFlowConditionPopup(type , customizationDetailsResponseData);
			}else if(heading === "moduleWorkflowFunctionCount" || heading === "wiWorkflowScriptFunctionCount"){
			showWorkFlowFunctionPopup(type , customizationDetailsResponseData);	
			}else{
				showCustomEnumerationModelPopup(type , customizationDetailsResponseData);
			}
        },
        error: function(error) {
            console.error("Error Message is", error);
        }
    });
});






function showCustomFieldModelPopup(wiType, customizationDetailsResponseData) {
    const modal = $('<div>').addClass('modal');
    const modalContent = $('<div>').addClass('modal-content');
    const popupHeading = $('<h4>').addClass('popup-heading').attr('id', 'popupHeading').text(wiType);
    const popupBody = $('<div>').addClass('popup-body').css({
        'max-height': '300px',
        'overflow-y': 'auto'
    });
    const table = $('<table>').addClass('table-main');
    const tbody = $('<tbody>').attr('id', 'popupDetailsTable');
    const tableHeaderRow = $('<tr>').addClass('table-header-row');
    tableHeaderRow.append($('<th>').text('Custom ID'));
    tableHeaderRow.append($('<th>').text('Custom Name'));
    tbody.append(tableHeaderRow);

 
    if (Object.keys(customizationDetailsResponseData).length > 8) {
        popupBody.css('max-height', '500px');
    }

    for (const key in customizationDetailsResponseData) {
        if (customizationDetailsResponseData.hasOwnProperty(key)) {
            const customDetail = customizationDetailsResponseData[key];
            const tableContentRow = $('<tr>').addClass('table-content-row');
            tableContentRow.append($('<td>').text(customDetail.customId));
            tableContentRow.append($('<td>').text(customDetail.customName));
            tbody.append(tableContentRow);
        }
    }

    table.append(tbody);
    popupBody.append(table);
    const popupFooter = $('<div>').addClass('popup-footer');
    const closeBtn = $('<span>').addClass('btn-popup-close').text('Close').on('click', function() {
        modal.hide();
    });
    popupFooter.append(closeBtn);
    modalContent.append(popupHeading, popupBody, popupFooter);
    modal.append(modalContent);
    $('#popup-modal').append(modal);
    modal.show();
}

function showWorkFlowConditionPopup(wiType, customizationDetailsResponseData) {

    const modal = $('<div>').addClass('modal');
    const modalContent = $('<div>').addClass('modal-content');
    const popupHeading = $('<h4>').addClass('popup-heading').attr('id', 'popupHeading').text(wiType);
    const popupBody = $('<div>').addClass('popup-body')
    const table = $('<table>').addClass('table-main');
    const tbody = $('<tbody>').attr('id', 'popupDetailsTable');
    const tableHeaderRow = $('<tr>').addClass('table-header-row');
    tableHeaderRow.append($('<th>').text('Action ID'));
    tableHeaderRow.append($('<th>').text('Action Name'));
    tableHeaderRow.append($('<th>').text('Script Name'));
    tbody.append(tableHeaderRow);

 
    if (Object.keys(customizationDetailsResponseData).length > 8) {
        popupBody.css('max-height', '500px'); 
    }

    for (const key in customizationDetailsResponseData) {
        if (customizationDetailsResponseData.hasOwnProperty(key)) {
            const customDetail = customizationDetailsResponseData[key];
            const tableContentRow = $('<tr>').addClass('table-content-row');
            tableContentRow.append($('<td>').text(customDetail.actionId));
            tableContentRow.append($('<td>').text(customDetail.actionName));
            tableContentRow.append($('<td>').text(customDetail.attachedJsFile));
            tbody.append(tableContentRow);
        }
    }

    table.append(tbody);
    popupBody.append(table);
    const popupFooter = $('<div>').addClass('popup-footer');
    const closeBtn = $('<span>').addClass('btn-popup-close').text('Close').on('click', function() {
        modal.hide();
    });
    popupFooter.append(closeBtn);
    modalContent.append(popupHeading, popupBody, popupFooter);
    modal.append(modalContent);
    $('#popup-modal').append(modal);
    modal.show();
}

function showWorkFlowFunctionPopup(wiType, customizationDetailsResponseData) {
    const modal = $('<div>').addClass('modal');
    const modalContent = $('<div>').addClass('modal-content');
    const popupHeading = $('<h4>').addClass('popup-heading').attr('id', 'popupHeading').text(wiType);
    const popupBody = $('<div>').addClass('popup-body');
    const table = $('<table>').addClass('table-main');
    const tbody = $('<tbody>').attr('id', 'popupDetailsTable');
    const tableHeaderRow = $('<tr>').addClass('table-header-row');
    tableHeaderRow.append($('<th>').text('Action ID'));
    tableHeaderRow.append($('<th>').text('Action Name'));
    tableHeaderRow.append($('<th>').text('Script Name'));
    tbody.append(tableHeaderRow);


    if (Object.keys(customizationDetailsResponseData).length > 8) {
        popupBody.css('max-height', '500px');
    }

    for (const key in customizationDetailsResponseData) {
        if (customizationDetailsResponseData.hasOwnProperty(key)) {
            const customDetail = customizationDetailsResponseData[key];
            const tableContentRow = $('<tr>').addClass('table-content-row');
            tableContentRow.append($('<td>').text(customDetail.actionId));
            tableContentRow.append($('<td>').text(customDetail.actionName));
            tableContentRow.append($('<td>').text(customDetail.attachedJsFile));
            tbody.append(tableContentRow);
        }
    }

    table.append(tbody);
    popupBody.append(table);
    const popupFooter = $('<div>').addClass('popup-footer');
    const closeBtn = $('<span>').addClass('btn-popup-close').text('Close').on('click', function() {
        modal.hide();
    });
    popupFooter.append(closeBtn);
    modalContent.append(popupHeading, popupBody, popupFooter);
    modal.append(modalContent);
    $('#popup-modal').append(modal);
    modal.show();
}


function showCustomEnumerationModelPopup(wiType, customizationDetailsResponseData) {
    const modal = $('<div>').addClass('modal');
    const modalContent = $('<div>').addClass('modal-content');
    const popupHeading = $('<h4>').addClass('popup-heading').attr('id', 'popupHeading').text(wiType);
    const popupBody = $('<div>').addClass('popup-body')
    const table = $('<table>').addClass('table-main');
    const tbody = $('<tbody>').attr('id', 'popupDetailsTable');
    const tableHeaderRow = $('<tr>').addClass('table-header-row');
    tableHeaderRow.append($('<th>').text('Enumeration Id'));
    tbody.append(tableHeaderRow);


    if (Object.keys(customizationDetailsResponseData).length > 8) {
        popupBody.css('max-height', '500px');
    }

    for (const key in customizationDetailsResponseData) {
        if (customizationDetailsResponseData.hasOwnProperty(key)) {
            const customDetail = customizationDetailsResponseData[key];
            const tableContentRow = $('<tr>').addClass('table-content-row');
            tableContentRow.append($('<td>').text(customDetail.customEnumeration));
            tbody.append(tableContentRow);
        }
    }

    table.append(tbody);
    popupBody.append(table);
    const popupFooter = $('<div>').addClass('popup-footer');
    const closeBtn = $('<span>').addClass('btn-popup-close').text('Close').on('click', function() {
        modal.hide();
    });
    popupFooter.append(closeBtn);
    modalContent.append(popupHeading, popupBody, popupFooter);
    modal.append(modalContent);
    $('#popup-modal').append(modal);
    modal.show();
}