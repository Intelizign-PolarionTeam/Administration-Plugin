$(document).ready(function() {
	$.ajax({
		url: 'custommanager?action=getProjectList',
		type: 'GET',
		dataType: 'json',
		success: function(response) {
			const projectsList = response.projectsList;

			if (response) {
				var projectSelect = $('.form-control');
				projectSelect.empty();
				projectSelect.append($('<option>', { value: '', text: '-- Select a Project --' }));
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
	if (!projectId) {
		alert("Please select a project");
		return;
	}
	//   $('.loader').css('display', 'block');

	$('#exportButton').css('display', 'block');
	$('#hideLicDiv').css('display', 'block');
	$('#hideUserDiv').css('display', 'block');
	$('.custom').css('display', 'block');
	$('.polarion-rpw-table-content').show();
	$('.export-div').show();
	$('.version-div').show();
	$('#popupModel').css('display', 'block');
	$.ajax({
		url: `custommanager?action=getCustomizationCountDetails&projectId=${projectId}`,
		type: "GET",
		dataType: "json",
		success: function(data) {

			const wiCustomizationObj = data.customizationCountDetails;
			const moduleCustomizationObj = data.moduleCustomizationDetails;
			const liveReportDetailsObj = data.liveReportDetailsResponse;
			const pluginDetailsObj = data.pluginDetails;
			const prePostSaveScriptObj = data.prePostSaveScriptDetails;
			const licenseDetailsObj = data.licenseDetails;
			const productId = data.productId;
			const versionId = data.versionId;


			$('#workItemTableBody').empty();
			$('#documentTableBody').empty();

			workItemCustomizationTable(wiCustomizationObj);
			moduleCustomizationTable(moduleCustomizationObj, versionId);
			liveReportCustomizationTable(liveReportDetailsObj);
			pluginDetailCustomizationTable(pluginDetailsObj);
			prePostSaveScriptMapCustomizationTable(prePostSaveScriptObj);
			getVersionDetailsCustomizationTable(licenseDetailsObj, productId, versionId);

			console.log("Plugin Details Obj is", pluginDetailsObj);
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
			row.append($('<td>').text(wiCustom.wiName).css('text-align', 'left'));

			['wiCustomFieldCount', 'customEnumerationCount', 'wiWorkflowScriptConditionCount', 'wiWorkflowScriptFunctionCount'].forEach(function(countType) {
				var count = wiCustom[countType];
				var countCell = $('<td>').css('text-align', 'center');
				if (count > 0) {
					var hyperlink = $('<a>').addClass('data-span clickable-cell').css('font-weight', 'bold')
						.text(count)
						.data('heading', countType)
						.data('type', wiCustom.wiType)
						.data('name', wiCustom.wiName);

					countCell.append(hyperlink);
				} else {
					countCell.text(count);
				}
				row.append(countCell);
			});

			$('#workItemTableBody').append(row);
		}
	});
}

function exportExcel() {
	var wb = XLSX.utils.book_new();
	var tables = document.getElementsByClassName('export-table');
	for (var i = 0; i < tables.length; i++) {
		var table = tables[i];
		var ws = XLSX.utils.table_to_sheet(table);
		var sheetName = table.caption ? table.caption.innerText : "Sheet" + (i + 1);
		XLSX.utils.book_append_sheet(wb, ws, sheetName);
		var range = XLSX.utils.decode_range(ws['!ref']);
		var colWidths = [];
		var rowHeights = [];
		for (var r = range.s.r; r <= range.e.r; r++) {
			for (var c = range.s.c; c <= range.e.c; c++) {
				var cellAddress = { c: c, r: r };
				var cell = ws[XLSX.utils.encode_cell(cellAddress)];
				if (cell) {
					var cellContent = cell.v ? cell.v.toString() : '';
					var cellWidth = cellContent.length * 1.1; // Adjust based on your preference
					colWidths[c] = colWidths[c] ? Math.max(colWidths[c], cellWidth) : cellWidth;
					var cellHeight = cellContent.split('\n').length * 15; // Adjust based on your preference
					rowHeights[r] = rowHeights[r] ? Math.max(rowHeights[r], cellHeight) : cellHeight;
				}
			}
		}
		ws['!cols'] = colWidths.map(function(width) { return { wch: width }; });
		ws['!rows'] = rowHeights.map(function(height) { return { hpx: height }; });
	}
	XLSX.writeFile(wb, 'customizationDetails.xlsx');
}

function moduleCustomizationTable(moduleCustomizationObj, versionId) {
    var versionIds = "22R1";
    var version = parseInt(versionIds.substring(0, 2));
    if (version >= 23) {
        $('#customizationmessage').html(`<span>*From Version 2304 Document Custom Field is applicable to each module type.</span>`);
    } else {
        $('#customizationmessage').html(`<span>*Below Version 2304 Document Custom Field is applicable all module type.</span>`);
    }

    // Check if version is less than 23 and there are elements in the moduleCustomizationObj array
    var shouldMergeCells = (version < 23 && moduleCustomizationObj.length > 0);

    $.each(moduleCustomizationObj, function(index, moduleCustom) {
        if (moduleCustom.hasOwnProperty('moduleType') && moduleCustom.hasOwnProperty('moduleName') && moduleCustom.hasOwnProperty('moduleCustomfieldCount') &&
            moduleCustom.hasOwnProperty('moduleWorkflowFunctionCount') && moduleCustom.hasOwnProperty('moduleWorkflowConditionCount')) {

            var row = $('<tr>').addClass('table-content-row');
            row.append($('<td>').text(moduleCustom.moduleName).css('text-align', 'left'));

            ['moduleCustomfieldCount', 'moduleWorkflowConditionCount', 'moduleWorkflowFunctionCount'].forEach(function(countType, columnIndex) {
                var count = moduleCustom[countType];
                var countCell = $('<td>').css('text-align', 'center');
                if (count > 0) {
                    var hyperlink = $('<a>').addClass('data-span clickable-cell').css('font-weight', 'bold')
                        .text(count)
                        .data('heading', countType)
                        .data('type', moduleCustom.moduleType)
                        .data('name', moduleCustom.moduleName);

                    countCell.append(hyperlink);
                } else {
                    countCell.text(count);
                }

                if (columnIndex === 0 && shouldMergeCells && index === 0) {
                    countCell.prop('rowspan', moduleCustomizationObj.length);
                } else if (columnIndex === 0 && shouldMergeCells) {
                    countCell.addClass('hidden'); // Hide the cell without removing it
                }

                row.append(countCell);
            });

            $('#documentTableBody').append(row);
        }
    });
}





function prePostSaveScriptMapCustomizationTable(prePostSaveScriptObj) {
	$('#pluginPrePostTableBody').empty();
	console.log('prePostSaveScriptObj ', prePostSaveScriptObj)
	$.each(prePostSaveScriptObj, function(index, prePostObj) {
		if (prePostObj.hasOwnProperty('Name') && prePostObj.hasOwnProperty('Extension')) {

			var row = $('<tr>').addClass('table-content-row');
			['Name', 'Extension'].forEach(function(prePostObjEvent) {
				var eventValue = prePostObj[prePostObjEvent];
				var countCell = $('<td>').css('text-align', 'left');
				if (eventValue > 0) {
					var hyperlink = $('<a>').addClass('data-span clickable-cell').css('font-weight', 'bold').text(eventValue);
					countCell.append(hyperlink);
				} else {
					countCell.text(eventValue);
				}
				row.append(countCell);
			});

			$('#pluginPrePostTableBody').append(row);
		}

	});
}


function pluginDetailCustomizationTable(pluginDetailsObj) {
	$('#pluginReportTableBody').empty();
	const uniqueFolderNames = {};

	$.each(pluginDetailsObj, function(index, pluginObj) {

		if (pluginObj.hasOwnProperty('pluginDetails') && pluginObj.hasOwnProperty('pluginPath')) {


			if (!uniqueFolderNames[pluginObj.pluginDetails]) {
				var row = $('<tr>').addClass('table-content-row');


				['pluginDetails', 'pluginPath'].forEach(function(pluginEvent) {
					var eventValue = pluginObj[pluginEvent];
					console.log("Event Value is", eventValue);
					var countCell = $('<td>').css('text-align', 'left');
					if (eventValue > 0) {
						var hyperlink = $('<a>').addClass('data-span clickable-cell').css('font-weight', 'bold').text(eventValue);
						countCell.append(hyperlink);
					} else {
						countCell.text(eventValue);
					}
					row.append(countCell);
				});

				$('#pluginReportTableBody').append(row);

			}
		}
	});
}


function liveReportCustomizationTable(liveReportObj) {

	$('#spaceReportTableBody').empty();
	const uniqueFolderNames = {};


	const projectId = $("#projectDropDown").val();

	$.each(liveReportObj, function(index, reportObj) {
		if (reportObj.hasOwnProperty('folderName') && reportObj.hasOwnProperty('createdDate') &&
			reportObj.hasOwnProperty('updatedDate') && reportObj.hasOwnProperty('reportName')) {

			if (!uniqueFolderNames[reportObj.folderName]) {

				var row = $('<tr>').addClass('table-content-row');
				row.append($('<td>').text(reportObj.folderName).css('text-align', 'center'));


				var mergedReportDetails = {
					reportName: '',
					createdDate: reportObj.createdDate,
					updatedDate: reportObj.updatedDate
				};


				$.each(liveReportObj, function(index, innerReportObj) {
					if (innerReportObj.folderName === reportObj.folderName) {
						var decodedReportName = decodeURIComponent(innerReportObj.reportName);
						mergedReportDetails.reportName += (index > 0 ? '<br>' : '') + '<a href="' + getReportUrl(projectId, reportObj.folderName, decodedReportName, innerReportObj.reportId) + '" target="_blank" style="text-decoration: none;color:#0c63e4 !important;">' + decodedReportName + '</a>'; // Append report name with hyperlink and styling
					}
				});


				['reportName', 'createdDate', 'updatedDate'].forEach(function(reportEvent) {
					var eventValue = mergedReportDetails[reportEvent];
					var countCell = $('<td>').css('text-align', 'center');
					countCell.html(eventValue ? eventValue : '-');
					row.append(countCell);
				});


				$('#spaceReportTableBody').append(row);


				uniqueFolderNames[reportObj.folderName] = true;
			}
		}
	});
}


function getReportUrl(projectId, spaceName, reportName, reportId) {
	var baseUrl = window.location.protocol + '//' + window.location.host;
	var polarionStartingUrl = '/polarion/#/project/';
	if (spaceName === '_default') {
		return baseUrl + polarionStartingUrl + projectId + '/wiki/' + reportId;
	} else {
		return baseUrl + polarionStartingUrl + projectId + '/wiki/' + spaceName + '/' + reportId;
	}
}






$(document).on('click', '.clickable-cell', function() {

	const projectId = $("#projectDropDown").val();
	var heading = $(this).data('heading');
	var type = $(this).data('type');
	var name = $(this).data('name');


	$.ajax({
		url: `custommanager?action=getCustomizationDetails&heading=${heading}&type=${type}&projectId=${projectId}`,
		method: 'GET',

		success: function(response) {

			const customizationDetailsResponseData = response.customizationDetailsResponseData;

			if (heading === "moduleCustomfieldCount" || heading === "wiCustomFieldCount") {
				showCustomFieldModelPopup(name, customizationDetailsResponseData);
			} else if (heading === "moduleWorkflowConditionCount" || heading === "wiWorkflowScriptConditionCount") {
				showWorkFlowConditionPopup(name, customizationDetailsResponseData);
			} else if (heading === "moduleWorkflowFunctionCount" || heading === "wiWorkflowScriptFunctionCount") {
				showWorkFlowFunctionPopup(name, customizationDetailsResponseData);
			} else {
				showCustomEnumerationModelPopup(name, customizationDetailsResponseData);
			}
		},
		error: function(error) {
			console.error("Error Message is", error);
		}
	});
});

function showCustomFieldModelPopup(name, customizationDetailsResponseData) {
	const modal = $('<div>').addClass('modal');
	const modalContent = $('<div>').addClass('modal-popup-content');
	const popupHeading = $('<h4>').addClass('popup-heading').attr('id', 'popupHeading').text(name);
	const popupBody = $('<div>').addClass('popup-body').css('max-height', '400px').css('overflow-y', 'auto');
	const table = $('<table>').addClass('table-main');
	const thead = $('<thead>');
	const tbody = $('<tbody>').attr('id', 'popupDetailsTable');
	const tableHeaderRow = $('<tr>').addClass('table-header-row');
	tableHeaderRow.append($('<th>').text('Custom ID'));
	tableHeaderRow.append($('<th>').text('Custom Name'));
	tableHeaderRow.append($('<th>').text('Custom Type'));
	thead.append(tableHeaderRow);
	table.append(thead);

	for (const key in customizationDetailsResponseData) {
		if (customizationDetailsResponseData.hasOwnProperty(key)) {
			const customDetail = customizationDetailsResponseData[key];
			const tableContentRow = $('<tr>').addClass('table-content-row');
			tableContentRow.append($('<td>').text(customDetail.customId));
			tableContentRow.append($('<td>').text(customDetail.customName));
			let customType = customDetail.customType || "Enum";
			const lastSegment = customType.split('.').pop();
			tableContentRow.append($('<td>').text(lastSegment));
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


function getVersionDetailsCustomizationTable(getVersionDetails, productId, versionId) {
	$("#productId").text(productId);
	$("#versionId").text(versionId);
	Object.entries(getVersionDetails).forEach(([key, value]) => {
		if (Object.keys(value).length > 0) {
			Object.entries(value).forEach(([property, propValue]) => {
				if (property === 'licenseType' || property === 'userCompany' || property === 'userName' || property === 'userEmail') {
					$('#' + property).text(propValue);
				}
			});
		}
	});
	$('.header-versioninfo-maindiv').show();
}

function showWorkFlowConditionPopup(name, customizationDetailsResponseData) {
	const modal = $('<div>').addClass('modal');
	const modalContent = $('<div>').addClass('modal-popup-content');
	const popupHeading = $('<h4>').addClass('popup-heading').attr('id', 'popupHeading').text(name);
	const popupBody = $('<div>').addClass('popup-body').css('max-height', '400px').css('overflow-y', 'auto'); // Add overflow-y and max-height properties
	const table = $('<table>').addClass('table-main');
	const thead = $('<thead>'); // Create thead element for table header
	const tbody = $('<tbody>').attr('id', 'popupDetailsTable');
	const tableHeaderRow = $('<tr>').addClass('table-header-row');
	tableHeaderRow.append($('<th>').text('Action ID'));
	tableHeaderRow.append($('<th>').text('Action Name'));
	tableHeaderRow.append($('<th>').text('Script Name'));
	thead.append(tableHeaderRow); // Append the header row to the thead element
	table.append(thead); // Append the thead element to the table

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

function showWorkFlowFunctionPopup(name, customizationDetailsResponseData) {
	const modal = $('<div>').addClass('modal');
	const modalContent = $('<div>').addClass('modal-popup-content');
	const popupHeading = $('<h4>').addClass('popup-heading').attr('id', 'popupHeading').text(name);
	const popupBody = $('<div>').addClass('popup-body').css('max-height', '400px').css('overflow-y', 'auto'); // Add overflow-y and max-height properties
	const table = $('<table>').addClass('table-main');
	const thead = $('<thead>'); // Create thead element for table header
	const tbody = $('<tbody>').attr('id', 'popupDetailsTable');
	const tableHeaderRow = $('<tr>').addClass('table-header-row');
	tableHeaderRow.append($('<th>').text('Action ID'));
	tableHeaderRow.append($('<th>').text('Action Name'));
	tableHeaderRow.append($('<th>').text('Script Name'));
	thead.append(tableHeaderRow); // Append the header row to the thead element
	table.append(thead); // Append the thead element to the table

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
		$('#popupModel').css('display', 'none');
	});
	popupFooter.append(closeBtn);
	modalContent.append(popupHeading, popupBody, popupFooter);
	modal.append(modalContent);
	$('#popup-modal').append(modal);
	modal.show();
}




function showCustomEnumerationModelPopup(name, customizationDetailsResponseData) {
	const modal = $('<div>').addClass('modal');
	const modalContent = $('<div>').addClass('modal-popup-content');
	const popupHeading = $('<h4>').addClass('popup-heading').attr('id', 'popupHeading').text(name);
	const popupBody = $('<div>').addClass('popup-body').css('max-height', '400px').css('overflow-y', 'auto'); // Add overflow-y and max-height properties
	const table = $('<table>').addClass('table-main');
	const thead = $('<thead>');
	const tbody = $('<tbody>').attr('id', 'popupDetailsTable');
	const tableHeaderRow = $('<tr>').addClass('table-header-row');
	tableHeaderRow.append($('<th>').text('Enumeration Id'));
	thead.append(tableHeaderRow);
	table.append(thead);

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


document.addEventListener('DOMContentLoaded', function() {

	document.getElementById('showContentButton').addEventListener('click', function() {
		var contentContainer = document.getElementById('contentFooter');
		const projectId = $("#projectDropDown").val();
		if (!projectId) {

			return;
		} else {
			if (contentContainer.classList.contains('hidden')) {
				contentContainer.classList.remove('hidden');
			} else {
				contentContainer.classList.remove('hidden');
			}
		}
	});
});