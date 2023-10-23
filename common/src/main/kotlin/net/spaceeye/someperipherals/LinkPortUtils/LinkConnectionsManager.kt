package net.spaceeye.someperipherals.LinkPortUtils

import java.util.concurrent.ConcurrentHashMap

class RequestsHolder {
    var status_request: LinkRequest? = null
    var raycast_request: LinkRequest? = null
}

class ResponseHolder {
    var status_response: LinkResponse? = null
    var raycast_response: LinkResponse? = null
}

class LinkConnectionsManager {
    val constant_pings = ConcurrentHashMap<String, LinkPing>()

    private val port_requests = ConcurrentHashMap<String, RequestsHolder>()
    private val link_response = ConcurrentHashMap<String, ResponseHolder>()

    fun clear() {
        constant_pings.clear()
        port_requests.clear()
        link_response.clear()
    }

    fun removeAll(k: String) {
        if (constant_pings.contains(k)) {constant_pings.remove(k)}
        if (port_requests .contains(k)) {port_requests.remove(k)}
        if (link_response .contains(k)) {link_response.remove(k)}
    }

    fun getRequests(k: String): RequestsHolder {
        var requests = port_requests[k]
        if (requests == null) {requests = RequestsHolder(); port_requests[k] = requests}
        return requests
    }

    fun makeRequest(k: String, request: LinkRequest) {
        var requests = port_requests[k]
        if (requests == null) {requests = RequestsHolder(); port_requests[k] = requests}

        when (request) {
            is LinkStatusRequest -> requests.status_request = request

            is LinkRaycastRequest -> requests.raycast_request = request
            is LinkBatchRaycastRequest -> requests.raycast_request = request

            else -> throw RuntimeException("Unknow type of request")
        }
    }

    fun getResponses(k: String): ResponseHolder {
        var responses = link_response[k]
        if (responses == null) {responses = ResponseHolder(); link_response[k] = responses}
        return responses
    }

    fun makeResponse(k: String, response: LinkResponse) {
        var responses = link_response[k]
        if (responses == null) {responses = ResponseHolder(); link_response[k] = responses}

        when (response) {
            is LinkStatusResponse -> responses.status_response = response

            is LinkRaycastResponse -> responses.raycast_response = response
            is LinkBatchRaycastResponse -> responses.raycast_response = response

            else -> throw RuntimeException("Unknown type of response")
        }
    }
}