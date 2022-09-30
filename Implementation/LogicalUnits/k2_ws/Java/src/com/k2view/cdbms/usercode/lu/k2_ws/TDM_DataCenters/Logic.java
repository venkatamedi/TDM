/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.TDM_DataCenters;

import com.k2view.cdbms.shared.user.WebServiceUserCode;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.desc;
import com.k2view.fabric.api.endpoint.Endpoint.MethodType;
import com.k2view.fabric.api.endpoint.Endpoint.Produce;
import com.k2view.fabric.api.endpoint.Endpoint.resultMetaData;
import com.k2view.fabric.api.endpoint.Endpoint.webService;

import static com.k2view.cdbms.usercode.common.TDM.TdmSharedUtils.getFabricResponse;
import static com.k2view.cdbms.usercode.common.TDM.TdmSharedUtils.wrapWebServiceResults;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends WebServiceUserCode {

	@desc("Gets the list of the Data Centers defined in the Fabric cluster.")
	@webService(path = "dataCenters", verb = {MethodType.GET}, version = "1", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON})
	@resultMetaData(mediaType = Produce.JSON, example = "{\r\n" +
			"  \"result\": [\r\n" +
			"    {\r\n" +
			"      \"notes\": \"local node\",\r\n" +
			"      \"effective_ip\": \"127.0.0.1\",\r\n" +
			"      \"logical_ids\": \"\",\r\n" +
			"      \"node_id\": \"fabric_debug\",\r\n" +
			"      \"dc\": \"DC1\",\r\n" +
			"      \"status\": \"ALIVE\"\r\n" +
			"    }\r\n" +
			"  ],\r\n" +
			"  \"errorCode\": \"SUCCESS\",\r\n" +
			"  \"message\": null\r\n" +
			"}")
	public static Object wsGetDataCenters() throws Exception {
		return wrapWebServiceResults("SUCCESS", null, getFabricResponse("clusterstatus;"));
	}

}
