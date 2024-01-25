$(document).ready(function() {
	var timestamp = new Date().getTime();
	var currentUrl = window.location.href;
	console.log("current url is:" + currentUrl);
	var queryString = currentUrl.split('?')[1];
	var params = new URLSearchParams(queryString);
	var scopeValue = params.get('scope');
	var url = "";

	if (params.has('scope')) {
		console.log("Scope parameter is present.");
		console.log("Scope is:" + scopeValue);
		var projectScope = scopeValue.split('project/')[1];
		var projectId = projectScope.split('/')[0];
		var url = `usermana?action=getUserDetails&projectId=${projectId}&_nocache=${timestamp}`;
		console.log("Url" + url);



		$.ajax({
			url: url,
			method: "GET",
			contentType: "application/json",
			success: function(data) {
				const userMap = data.userMap;
				populateTable(userMap);
				console.log("UserMap is:" + userMap);
			},

			error: function(xhr, status, error) {
				console.error("Error message is", error);
			}

		});

	} else {
		console.log("Scope parameter is not present.");
	}

});

function populateTable(userMap) {

	const userTableBody = $("#userTableBody");
	let isLightRow = true;


	userTableBody.empty();
	for (const userId in userMap) {

		if (userMap.hasOwnProperty(userId)) {
			const userName = userMap[userId];
			const newRow = $("<tr>");

			newRow.addClass(isLightRow ? "polarion-rpw-table-content-row-light" : "polarion-rpw-table-content-row-dark");
			isLightRow = !isLightRow;

			const cellId = $("<td>").text(userId);
			const cellName = $("<td>").text(userName);
			
			newRow.append(cellId, cellName);
			userTableBody.append(newRow);

		}

	}

}
