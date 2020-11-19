
const listContainer = document.querySelector('#service-list');
let servicesRequest = new Request('/service');
fetch(servicesRequest)
.then(function(response) { return response.json(); })
.then(function(serviceList) {
  serviceList.forEach(service => {
    var li = document.createElement("li");
    var deleteBtn = document.createElement("button");
    deleteBtn.innerHTML = "Delete";
    deleteBtn.onclick = function() {deleteService(service.url)};
    deleteBtn.style.marginRight = "10px";
    li.appendChild(deleteBtn);
    li.appendChild(document.createTextNode(service.url + ';' + service.name + ';'+ service.status));
    listContainer.appendChild(li);
  });
});

const saveButton = document.querySelector('#post-service');
saveButton.onclick = evt => {
    let urlName = document.querySelector('#url-name').value;
    let serviceName = document.querySelector('#service-name').value;

    if (!urlName.startsWith('https://') && !urlName.startsWith('http://')) {
                urlName = 'https://' + urlName;
            }
    fetch('/service', {
    method: 'post',
    headers: {
    'Accept': 'application/json, text/plain, */*',
    'Content-Type': 'application/json'
    },
  body: JSON.stringify({url:urlName,name:serviceName})
}).then(res=> location.reload());
}

function deleteService(urlName) {
    postToEndpoint('/delete', urlName);
}

function postToEndpoint(endpoint, urlName) {
    fetch(endpoint, {
            method: 'post',
            headers: {
            'Accept': 'application/json, text/plain, */*',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({url:urlName})
        }).then(res => location.reload());
}
