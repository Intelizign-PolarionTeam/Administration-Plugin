$(document).ready(function() {
	$.ajax({
		url: 'usermana?action=getProjectList',
		type: 'GET',
		dataType: 'json',
		success: function(response) {
			const projectsList = response.projectsList;
			//const prePostHookMapObj = response.prePostHookMapObj
			if (response) {
				var projectSelect = $('.form-control');
				projectSelect.empty();
				projectSelect.append($('<option>', { value: '', text: '-- Select a project --' }));
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
	$('#accordionExample .accordion-collapse').collapse('show');
	const projectId = $("#projectDropDown").val();
	if (!projectId) {
        alert("Please select a project");
        return; 
    }
$('.custom').css('display', 'block');
	$('.polarion-rpw-table-content').show();
	$('.export-div').show();
	$('.version-div').show();
	$('#pre-post').css('display', 'block');
	$.ajax({
		url: `usermana?action=getCustomizationCountDetails&projectId=${projectId}`,
		type: "GET",
		dataType: "json",
		success: function(data) {

			const wiCustomizationObj = data.customizationCountDetails;
			const moduleCustomizationObj = data.moduleCustomizationDetails;
			const liveReportDetailsObj = data.liveReportDetailsResponse;
			const pluginDetailsObj= data.pluginDetailsMap;
			const prePostSaveScriptObj = data.prePostSaveScriptMap;
			const getVersionDetails = data.getVersionDetails;
			
			// Clear existing table content
			$('#userTableBody').empty();
			$('#userTableBodyDocument').empty();
			//$('#liveReportTableBody').empty();
			workItemCustomizationTable(wiCustomizationObj);
			moduleCustomizationTable(moduleCustomizationObj);
			liveReportCustomizationTable(liveReportDetailsObj);
			pluginDetailCustomizationTable(pluginDetailsObj);
			prePostSaveScriptMapCustomizationTable(prePostSaveScriptObj);
			getVersionDetailsCustomizationTable(getVersionDetails);
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

 function exportExcel()
{
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
                var cellAddress = {c: c, r: r};
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

function moduleCustomizationTable(moduleCustomizationObj) {
	$.each(moduleCustomizationObj, function(index, moduleCustom) {
		

		if (moduleCustom.hasOwnProperty('moduleType') && moduleCustom.hasOwnProperty('moduleName') && moduleCustom.hasOwnProperty('moduleCustomfieldCount') &&
			moduleCustom.hasOwnProperty('moduleWorkflowFunctionCount') && moduleCustom.hasOwnProperty('moduleWorkflowConditionCount')) {

			
			var row = $('<tr>').addClass('table-content-row');
			row.append($('<td>').text(moduleCustom.moduleName).css('text-align', 'left'));

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

function getVersionDetailsCustomizationTable(getVersionDetails) {
    $('#versionTableBody').empty();

    // Extract the values of getVersionDetails object into an array
    var values = Object.values(getVersionDetails);

    // Define an array to store the rows
    var rows = [];

    // Iterate over the values array
    for (var i = 0; i < values.length; i++) {
        var versionObj = values[i];

        // Create a new row for every 2 elements
        if (i % 2 === 0) {
            var row = $('<tr>').addClass('table-content-row');
            rows.push(row);
        }

        // Check if the versionObj is not empty and contains the required properties
        if (Object.keys(versionObj).length > 0) {
            // Loop through the properties of versionObj and create cells
            Object.entries(versionObj).forEach(([property, value]) => {
                var propertyCell = $('<td>').css({
                    'text-align': 'center',
                    'font-size': '9px',
                    'font-weight': 'bold'
                }).text(property);

                var valueCell = $('<td>').css({
                    'text-align': 'center',
                    'font-size': '9px'
                }).text(value);

                // Append the cells to the last row in the rows array
                rows[rows.length - 1].append(propertyCell, valueCell);
            });
        }
    }

    // Append all rows to the table body
    $('#versionTableBody').append(rows);
}








function prePostSaveScriptMapCustomizationTable(prePostSaveScriptObj)
{
	 $('#prePostReportTableBody').empty();
	  $.each(prePostSaveScriptObj, function(index, prePostObj) {
        if (prePostObj.hasOwnProperty('Name') && prePostObj.hasOwnProperty('Extension')) {
           
            
                var row = $('<tr>').addClass('table-content-row');
              

                ['Name', 'Extension'].forEach(function(prePostObjEvent) {
                    var eventValue = prePostObj[prePostObjEvent];
                    var countCell = $('<td>').css('text-align', 'center');
                    if (eventValue > 0) {
                        var hyperlink = $('<a>').addClass('data-span clickable-cell').text(eventValue);
                        countCell.append(hyperlink);
                    } else {
                        countCell.text(eventValue);
                    }
                    row.append(countCell);
                });

                $('#prePostReportTableBody').append(row);
                //uniqueFolderNames[pluginObj.pluginDeatils] = true;
            }
        
    });
}


function pluginDetailCustomizationTable(pluginDetailsObj) {
	$('#pluginReportTableBody').empty();
    const uniqueFolderNames = {};

    $.each(pluginDetailsObj, function(index, pluginObj) {
        
        if (pluginObj.hasOwnProperty('pluginDeatils') && pluginObj.hasOwnProperty('pluginPath')) {
            

            if (!uniqueFolderNames[pluginObj.pluginDeatils]) {
                var row = $('<tr>').addClass('table-content-row');
              

                ['pluginDeatils', 'pluginPath'].forEach(function(pluginEvent) {
                    var eventValue = pluginObj[pluginEvent];
                    var countCell = $('<td>').css('text-align', 'center');
                    if (eventValue > 0) {
                        var hyperlink = $('<a>').addClass('data-span clickable-cell').text(eventValue);
                        countCell.append(hyperlink);
                    } else {
                        countCell.text(eventValue);
                    }
                    row.append(countCell);
                });

                $('#pluginReportTableBody').append(row);
                //uniqueFolderNames[pluginObj.pluginDeatils] = true;
            }
        }
    });
}


function liveReportCustomizationTable(liveReportObj) {
    // Object to keep track of unique folder names
    $('#liveReportTableBody').empty();
    const uniqueFolderNames = {};

    // Get projectId from projectDropDown
    const projectId = $("#projectDropDown").val();

    $.each(liveReportObj, function(index, reportObj) {
        if (reportObj.hasOwnProperty('folderName') && reportObj.hasOwnProperty('createdDate') &&
            reportObj.hasOwnProperty('updatedDate') && reportObj.hasOwnProperty('reportName')) {

            // Check if folder name is already encountered
            if (!uniqueFolderNames[reportObj.folderName]) {
                // Initialize a row for the folder
                var row = $('<tr>').addClass('table-content-row');
                row.append($('<td>').text(reportObj.folderName).css('text-align', 'center'));

                // Initialize an object to hold merged report details
                var mergedReportDetails = {
                    reportName: '',
                    createdDate: reportObj.createdDate,
                    updatedDate: reportObj.updatedDate
                };

                // Loop through all reports and merge their details for the same folder
                $.each(liveReportObj, function(index, innerReportObj) {
                    if (innerReportObj.folderName === reportObj.folderName) {
                        // Decode the report name before generating the URL
                        var decodedReportName = decodeURIComponent(innerReportObj.reportName);
                        mergedReportDetails.reportName += (index > 0 ? '<br>' : '') + '<a href="' + getReportUrl(projectId, reportObj.folderName, decodedReportName) + '" target="_blank" style="text-decoration: none; font-weight: bold; color: #005F87;">' + decodedReportName + '</a>'; // Append report name with hyperlink and styling
                    }
                });

                // Add merged report details to the row
                ['reportName', 'createdDate', 'updatedDate'].forEach(function(reportEvent) {
                    var eventValue = mergedReportDetails[reportEvent];
                    var countCell = $('<td>').css('text-align', 'center');
                    countCell.html(eventValue ? eventValue : '-'); // Display the report name as HTML
                    row.append(countCell);
                });

                // Append the row to the table body
                $('#liveReportTableBody').append(row);

                // Mark folder name as encountered
                uniqueFolderNames[reportObj.folderName] = true;
            }
        }
    });
}

// Function to get report URL
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
    //alert("Selected project Id is: " + projectId);
    var heading = $(this).data('heading');
    var type = $(this).data('type');
   

    $.ajax({
        url: `usermana?action=getCustomizationDetails&heading=${heading}&type=${type}&projectId=${projectId}`,
        method: 'GET',

        success: function(response) {
           
            const customizationDetailsResponseData = response.customizationDetailsResponseData;
           
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
	
   
    $('#popupModel').css('display', 'block');
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
     tableHeaderRow.append($('<th>').text('Custom Type'));
    tbody.append(tableHeaderRow);

 
    if (Object.keys(customizationDetailsResponseData).length > 8) {
        popupBody.css('max-height', '500px');
    }

    for (const key in customizationDetailsResponseData) {
        if (customizationDetailsResponseData.hasOwnProperty(key)) {
            const customDetail = customizationDetailsResponseData[key];
            console.log("customDetail ",customDetail)
            const tableContentRow = $('<tr>').addClass('table-content-row');
            tableContentRow.append($('<td>').text(customDetail.customId));
            tableContentRow.append($('<td>').text(customDetail.customName));
             let customType = customDetail.customType || "Enum"; 
            const lastSegment = customType.split('.').pop();
            tableContentRow.append($('<td>').text(lastSegment));
            tbody.append(tableContentRow);
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
    $('#pre-post').append(modal);
    modal.show();
}

function showWorkFlowConditionPopup(wiType, customizationDetailsResponseData) {
	
    $('#popupModel').css('display', 'block');
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
        $('#popupModel').css('display', 'none');
    });
    popupFooter.append(closeBtn);
    modalContent.append(popupHeading, popupBody, popupFooter);
    modal.append(modalContent);
    $('#pre-post').append(modal);
    modal.show();
}

function showWorkFlowFunctionPopup(wiType, customizationDetailsResponseData) {
	
    $('#popupModel').css('display', 'block');
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
        $('#popupModel').css('display', 'none');
    });
    popupFooter.append(closeBtn);
    modalContent.append(popupHeading, popupBody, popupFooter);
    modal.append(modalContent);
    $('#pre-post').append(modal);
    modal.show();
}

function showCustomEnumerationModelPopup(wiType, customizationDetailsResponseData) {
	
    $('#popupModel').css('display', 'block');
    const modal = $('<div>').addClass('modal');
    const modalContent = $('<div>').addClass('modal-content').css({
        'width': '80%'
        
    });;
    
    const popupHeading = $('<h4>').addClass('popup-heading').attr('id', 'popupHeading').text(wiType);
    const popupBody = $('<div>').addClass('popup-body').css({
        'max-height': '300px',
        'overflow-y': 'auto'
    });
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
        $('#popupModel').css('display', 'none');
    });
    popupFooter.append(closeBtn);
    modalContent.append(popupHeading, popupBody, popupFooter);
    modal.append(modalContent);
    $('#pre-post').append(modal);
    modal.show();
}