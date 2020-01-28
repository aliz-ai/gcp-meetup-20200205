fetch('/tictac')
	.then(response => response.text())
	.then(responseText => {
		console.log("response", responseText);
	});

function place(index) {
	fetch('/tictac?place=' + index)
		.then(response => response.text())
		.then(responseText => {
			console.log("response", responseText);
		});
}