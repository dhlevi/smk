package ca.bc.gov.databc.smks.model.layer;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import ca.bc.gov.databc.smks.model.FeatureLayer;

@JsonInclude(Include.NON_NULL)
public class EsriDynamic extends FeatureLayer
{
	private Integer mpcmId;
	private String mpcmWorkspace;
	private String serviceUrl;
	private List<String> dynamicLayers;

	public EsriDynamic() { }

	protected EsriDynamic( EsriDynamic layer ) {
		super( layer );

		this.setMpcmId(layer.getMpcmId());
		this.setMpcmWorkspace(layer.getMpcmWorkspace());
		this.setServiceUrl(layer.getServiceUrl());

		for(String s : layer.getDynamicLayers())
		{
			this.getDynamicLayers().add(s);
		}
	}

	public Integer getMpcmId()
	{
		return mpcmId;
	}

	public void setMpcmId(Integer mpcmId)
	{
		this.mpcmId = mpcmId;
	}

	public String getMpcmWorkspace()
	{
		return mpcmWorkspace;
	}

	public void setMpcmWorkspace(String mpcmWorkspace)
	{
		this.mpcmWorkspace = mpcmWorkspace;
	}

	public String getServiceUrl()
	{
		return serviceUrl;
	}

	public void setServiceUrl(String serviceUrl)
	{
		this.serviceUrl = serviceUrl;
	}

	public List<String> getDynamicLayers()
	{
		if(dynamicLayers == null) dynamicLayers = new ArrayList<String>();
		return dynamicLayers;
	}

	public void setDynamicLayers(List<String> dynamicLayers)
	{
		this.dynamicLayers = dynamicLayers;
	}

	public EsriDynamic clone()
	{
		EsriDynamic clone = new EsriDynamic( this );

		return clone;
	}
}
