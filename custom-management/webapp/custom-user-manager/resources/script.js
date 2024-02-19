$(document).ready(function() {
	$.ajax({
		url: 'usermana?action=getProjectList',
		type: 'GET',
		dataType: 'json',
		success: function(response) {
			const projectsList = response.projectsList;
			const prePostHookMapObj = response.prePostHookMapObj		
			$('#pre-post a').on('click', function() {
				showPopupForPrePost('Pre-Post Save Details', prePostHookMapObj);
			});
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
		error: function(xhr, status, error) {
			console.error('Error occurred while fetching project list:', error);
		}
	});
});



function projectInfo() {
    const projectId = $("#projectDropDown").val();
    alert("Selected project Id is: " + projectId);
    $('.polarion-rpw-table-content').show();
    const tbody = $('#userTableBody');
    $('#pre-post').css('display', 'block');
    $.ajax({
        url: `usermana?action=getCustomizationDetails&projectId=${projectId}`,
        type: "GET",
        dataType: "json",
        success: function(data) {
            console.log("Response data:", data); // Log the entire response data
            const wiCustomizationObj = data.customizationDetails;
            const moduleCustomizationObj = data.moduleCustomizationDetails;
            // Clear existing table content
            $('#userTableBody').empty();
            $('#userTableBodyDocument').empty();

            $.each(wiCustomizationObj, function(index, wiCustom) {
                console.log("wiCustom object:", wiCustom); // Log the entire object
                if (wiCustom.hasOwnProperty('wiType') && wiCustom.hasOwnProperty('wiName') &&
                    wiCustom.hasOwnProperty('customFieldCount') && wiCustom.hasOwnProperty('scriptcount') &&
                    wiCustom.hasOwnProperty('customEnumerationCount') && wiCustom.hasOwnProperty('scriptFunctionCount')) {

                    console.log("wiName is", wiCustom.wiName);
                    var row = $('<tr>').addClass('table-content-row');
                    row.append($('<td>').text(wiCustom.wiName).css('text-align', 'center'));

                    // Process each count cell
                    ['customFieldCount', 'scriptcount', 'customEnumerationCount', 'scriptFunctionCount'].forEach(function(countType) {
                        var count = wiCustom[countType];
                        var countCell = $('<td>').css('text-align', 'center');
                        if (count > 0) {
                            var hyperlink = $('<a>').addClass('clickable-cell').css({
                                'color': 'rgb(83, 83, 245)',
                                'cursor': 'pointer',
                                'text-decoration': 'none' // Remove underline
                            }).text(count);
                            hyperlink.data('rowData', wiCustom);
                            countCell.append(hyperlink);
                        } else {
                            countCell.text(count);
                        }
                        row.append(countCell);
                    });

                    $('#userTableBody').append(row);
                }
            });
            documentInfo(moduleCustomizationObj);
        },
        error: function(xhr, status, error) {
            console.error("Error occurred:", error);
        }
    });
}

$(document).on('click', '.clickable-cell', function() {
    var rowData = $(this).data('rowData');
    showModalPopup(rowData);
});

function documentInfo(moduleCustomizationObj) {
    $.each(moduleCustomizationObj, function(index, moduleCustom) {
        console.log("moduleCustom object:", moduleCustom); // Log the entire object
        if (moduleCustom.hasOwnProperty('moduleType') && moduleCustom.hasOwnProperty('moduleCustomFieldCount') &&
            moduleCustom.hasOwnProperty('moduleWorkflowConditionCount') && moduleCustom.hasOwnProperty('moduleWorkflowFunctionCount')) {

            console.log("moduleType is", moduleCustom.moduleType);
            var row = $('<tr>').addClass('table-content-row');
            row.append($('<td>').text(moduleCustom.moduleType).css('text-align', 'center'));

            // Process each count cell
            ['moduleCustomFieldCount', 'moduleWorkflowConditionCount', 'moduleWorkflowFunctionCount'].forEach(function(countType) {
                var count = moduleCustom[countType];
                var countCell = $('<td>').css('text-align', 'center');
                if (count > 0) {
                    var hyperlink = $('<a>').addClass('clickable-cell').css({
                        'color': 'rgb(83, 83, 245)',
                        'cursor': 'pointer',
                        'text-decoration': 'none' // Remove underline
                    }).text(count);
                    hyperlink.data('rowData', moduleCustom);
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


$(document).on('click', '.clickable-cell', function() {
	var rowData = $(this).data('rowData');
	showModalPopup(rowData);
});

function showModalPopup(rowData) {
	$('#modalTitle').text(rowData.wiName + ' Details');
	$('#customFieldCount').text(rowData.customFieldCount);
	$('#scriptcount').text(rowData.scriptcount);
	$('#customEnumerationCount').text(rowData.customEnumerationCount);
	$('#scriptFunctionCount').text(rowData.scriptFunctionCount);
	// Add other details to the modal

	$('#myModal').show()
}




$(document).on('click', '.customFieldCount, .customEnumerationCount, .scriptFunctionCount, .scriptCount,.customFieldListModuleCount,.scriptDocumentFunctionCount,.scriptDocumentCount', function(e) {
	e.preventDefault();

	var type = $(this).data('type');
	var column = $(this).data('column');
	var projectid = $("#projectDropDown").val();
	var columnName = $(this).closest('table').find('th').eq($(this).closest('td').index()).text().trim();

	$.ajax({
		url: `usermana?action=getCustomizationDetailsPopup&type=${type}&projectid=${projectid}&column=${columnName}`, // Include type and column parameters in the URL
		type: "GET",
		dataType: "json",
		success: function(data) {
			const customFields = data.customFields;
			const customNames = data.customFieldNames;
			const workItemType = data.type;
			var actionScripts = data.actionScripts;
			var typeIdPrefixList = data.typeIdPrefixList;
			console.log("data ", data)
			$('#pre-post').css('display', 'block');

			if (Object.keys(actionScripts).length > 0) {
				showPopup('Action Scripts', Object.entries(actionScripts), 'Action ID', 'Script Name');
			} else if (Object.keys(customFields).length > 0) {
				showPopup('Custom Fields', Object.entries(customFields), 'Custom ID', 'Custom Name');
			} else if (Object.keys(typeIdPrefixList).length > 0) {
				showPopupForCustomEnumeration(typeIdPrefixList, workItemType);
			}
		},

		error: function(xhr, status, error) {
			console.error("Error occurred:", error);
		}
	});
});

function showPopup(scriptType, scriptList, headerText1, headerText2) {
	$('#popupModel').css('display', 'block');
	const modal = $('<div>').addClass('modal');
	const modalContent = $('<div>').addClass('modal-content');
	const popupHeading = $('<h4>').addClass('popup-heading').attr('id', 'popupHeading').text(scriptType);

	const popupBody = $('<div>').addClass('popup-body').css({
		'max-height': '300px',
		'overflow-y': 'auto'
	});

	const table = $('<table>').addClass('table-main');

	const tableHeader = $('<thead>');
	const tableHeaderRow = $('<tr>').addClass('table-header-row');
	tableHeaderRow.append($('<th>').text(headerText1));
	if (headerText2) {
		tableHeaderRow.append($('<th>').text(headerText2));
	}
	tableHeader.append(tableHeaderRow);

	const tableBody = $('<tbody>').attr('id', 'popupDetailsTable');
	scriptList.forEach(item => {
		const tableContentRow = $('<tr>').addClass('table-content-row');
		tableContentRow.append($('<td>').text(item[0]));
		if (item[1] && headerText2) {
			tableContentRow.append($('<td>').text(item[1]));
		}
		tableBody.append(tableContentRow);
	});

	table.append(tableHeader, tableBody);
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

function showPopupForCustomEnumeration(typeIdPrefixList, workItemType) {
	$('#popupModel').css('display', 'block');
	const modal = $('<div>').addClass('modal');
	const modalContent = $('<div>').addClass('modal-content');
	const popupHeading = $('<h4>').addClass('popup-heading').text(workItemType);

	const popupBody = $('<div>').addClass('popup-body').css({
		'max-height': '300px',
		'overflow-y': 'auto'
	});

	const table = $('<table>').addClass('table-main');

	const tableHeader = $('<thead>');
	const tableHeaderRow = $('<tr>').addClass('table-header-row');
	tableHeaderRow.append($('<th>').text('Enumeration Value'));
	tableHeader.append(tableHeaderRow);

	const tableBody = $('<tbody>').attr('id', 'popupDetailsTable');
	for (const value of typeIdPrefixList) {
		const tableContentRow = $('<tr>').addClass('table-content-row');
		tableContentRow.append($('<td>').text(value));
		tableBody.append(tableContentRow);
	}

	table.append(tableHeader, tableBody);
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

function showPopupForPrePost(scriptType, scriptList) {
	$('#popupModel').css('display', 'block');
	const modal = $('<div>').addClass('modal');
	const modalContent = $('<div>').addClass('modal-content');
	const popupHeading = $('<h4>').addClass('popup-heading').attr('id', 'popupHeading').text(scriptType);
	const popupBody = $('<div>').addClass('popup-body').css({
		'max-height': '300px',
		'overflow-y': 'auto'
	});
	const table = $('<table>').addClass('table-main');
	const tbody = $('<tbody>').attr('id', 'popupDetailsTable');
	const tableHeaderRow = $('<tr>').addClass('table-header-row');
	tableHeaderRow.append($('<th>').text('Script Name'));
	tbody.append(tableHeaderRow);

	// Check if the number of items in scriptList is greater than 8
	if (Object.keys(scriptList).length > 8) {
		popupBody.css('max-height', '500px'); // Adjust the max-height as needed
	}

	for (const fileName in scriptList) {
		const tableContentRow = $('<tr>').addClass('table-content-row');
		tableContentRow.append($('<td>').text(fileName));
		tbody.append(tableContentRow);
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


