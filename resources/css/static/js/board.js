function allowDrop(ev) {
    ev.preventDefault();
}

function drag(ev) {
    ev.dataTransfer.setData("text", ev.target.id);
}

function drop(ev) {
    ev.preventDefault();
    const taskId = ev.dataTransfer.getData("text");
    const targetColumn = ev.target.closest('.card-list');
    if (targetColumn && taskId) {
        targetColumn.appendChild(document.getElementById(taskId));

        //Send status update to backend
        fetch('/tasks/update-status', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                id: taskId,
                status: targetColumn.id.replace(/([A-Z])/g, ' $1').trim()
            })
        });
    }
}
