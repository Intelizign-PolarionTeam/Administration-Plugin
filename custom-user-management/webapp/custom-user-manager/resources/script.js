$(document).ready(function() {
	$.ajax({
		url: "usermana?action=getProjectList",
		type: "GET",
		dataType: "json",
		success: function(data) {
			const projectId = data.projectMap;
			console.log("Data is", projectId);

			const selectElement = $("#projectDropDown");
			selectElement.empty();

			try {
				// Use Object.entries() to get an array of [key, value] pairs from the projectId object
				for (const [key, name] of Object.entries(projectId)) {
					const option = $("<option>").val(key).text(name);
					selectElement.append(option);
				}
			} catch (error) {
				console.error("Error processing data:", error);
			}
		},
		error: function(error) {
			console.error("Error message is", error.statusText);
		}
	});
});



function projectInfo() {

	const projectId = $("#projectDropDown").val();
	alert("selected project Id is:" + projectId);
	$('.polarion-rpw-table-content').show();
	const tbody = $('#userTableBody');



	$.ajax({
		url: `usermana?action=getCustomizationDetails&projectId=${projectId}`,
		type: "GET",
		dataType: "json",

		success: function(data) {

			const customDetailsList = data.customDetailsList;
			const scriptConditionList = data.scriptConditionList;
			const scriptFunctionList = data.scriptFunctionList;
			const customEnumerationList = data.customEnumerationList;
			const customPostAndPreSaveScriptList = data.customPostAndPreSaveScriptList
//console.log("customPostAndPreSaveScriptList", customPostAndPreSaveScriptList);
const firstItemJsFileNames = customPostAndPreSaveScriptList[0].jsFileNames;
const jsFileCount = customPostAndPreSaveScriptList[0].jsFileCount;
console.log("jsFileNames for the first item:", customPostAndPreSaveScriptList);
console.log("jsFileNames ", firstItemJsFileNames);
console.log("jsFileCount ", jsFileCount);


for (const fileName of firstItemJsFileNames) {
    console.log("File Name:", fileName);
}

            createScriptLink("Pre-Post Save Details", firstItemJsFileNames, jsFileCount);
            


// Parse the JSON string into a JavaScript object
//const parsedData = JSON.parse(customPostAndPreSaveScriptList);

// Access the value of the 'jsname' property
//const jsnameValue = parsedData.jsname;
//console.log("jsnameValue", jsnameValue);

			tbody.empty();

			for (let i = 0; i < customDetailsList.length; i++) {
				const row = $('<tr>');
				row.append($('<td>').text(customDetailsList[i].workItemType));
				const customFieldCell = $('<td>');
				const customFieldButton = $('<button>').text(customDetailsList[i].customFieldsCount).addClass("customField-td-button");
				customFieldCell.append(customFieldButton);
				row.append(customFieldCell);

				// Update this block to use a helper function

				updateCellBasedOnEnumCondition(customEnumerationList, customDetailsList[i].workItemType, 'customEnumerationCount', row);
				updateCellBasedOnScriptCondition(scriptFunctionList, customDetailsList[i].workItemType, 'scriptFunctionCount', row);
				updateCellBasedOnScriptFunction(scriptConditionList, customDetailsList[i].workItemType, 'scriptCount', row);
				tbody.append(row);
			}

		},

		error: function(error) {
			console.error("Error message is", error);

		}

	});
}

// Helper function to update the cell based on condition

function updateCellBasedOnEnumCondition(list, workItemType, countProperty, row) {
	const cell = $('<td>');
	const matchingItem = list.find(item => item.customWorkItemType === workItemType);
	if (matchingItem) {
		const button = $('<button>').text(matchingItem[countProperty]).addClass("customField-td-button");
		cell.append(button);
	} else {
		const button = $('<button>').text(0).addClass("customField-td-button");
		cell.append(button);
	}
	row.append(cell);
}

// Helper function to update the cell based on condition

function updateCellBasedOnScriptCondition(list, workItemType, countProperty, row) {
	const cell = $('<td>');
	const matchingItem = list.find(item => item.scriptWorkItemType === workItemType);
	if (matchingItem) {
		console.log("Matching Item", matchingItem);
		const button = $('<button>').text(matchingItem[countProperty]).addClass("customField-td-button");
		cell.append(button);
	} else {
		const button = $('<button>').text(0).addClass("customField-td-button");
		cell.append(button);
	}
	row.append(cell);
}


// Helper function to update the cell based on condition

function updateCellBasedOnScriptFunction(list, workItemType, countProperty, row) {
	const cell = $('<td>');
	const matchingItem = list.find(item => item.scriptWorkItemType === workItemType);
	if (matchingItem) {
		console.log("Matching Item", matchingItem);
		const button = $('<button>').text(matchingItem[countProperty]).addClass("customField-td-button");
		cell.append(button);
	} else {
		const button = $('<button>').text(0).addClass("customField-td-button");
		cell.append(button);
	}
	row.append(cell);
}

function createScriptLink(scriptType, fileNames, jsFileCount) {
	 $('#prepost').empty();
    const link = $('<a>')
        .text(`${scriptType}`)
        .attr('href', '#') // Set href to "#" to prevent the page from navigating
        .on('click', function () {
            showPopup(scriptType, fileNames, jsFileCount);
        });

    // Append the link to a container or wherever you want it in your UI
    $('#prepost').append(link);
}

// Helper function to show modal popup with jsFileCount and fileName
function showPopup(scriptType, fileNames, jsFileCount) {
    // Create a modal popup
    $('#popupModel').css('display', 'block');
    const modal = $('<div>').addClass('modal');
    const modalContent = $('<div>').addClass('modal-content');
    const popupHeading = $('<h4>').addClass('popup-heading').attr('id', 'popupHeading').text('Pre - Post Save Details');

    // Create the body of the modal
    const popupBody = $('<div>').addClass('popup-body');
    const table = $('<table>').addClass('table-main');
    const tbody = $('<tbody>').attr('id', 'popupDetailsTable');

    // Add table headers
    const tableHeaderRow = $('<tr>').addClass('table-header-row');
    tableHeaderRow.append($('<th>').text('Script Name'));
    tbody.append(tableHeaderRow);

    // Add table content
    fileNames.forEach(fileName => {
        const tableContentRow = $('<tr>').addClass('table-content-row');
        tableContentRow.append($('<td>').text(fileName));
        tbody.append(tableContentRow);
    });

    table.append(tbody);
    popupBody.append(table);

    // Create the footer of the modal
    const popupFooter = $('<div>').addClass('popup-footer');
    const closeBtn = $('<span>').addClass('btn-popup-close').text('Close').on('click', function () {
        modal.hide();
        $('#popupModel').css('display', 'none');
    });

    popupFooter.append(closeBtn);

    // Assemble the modal content
    modalContent.append(popupHeading, popupBody, popupFooter);
    modal.append(modalContent);

    // Append the modal to the body
    $('#prepost').append(modal);

    // Show the modal
    modal.show();
}
