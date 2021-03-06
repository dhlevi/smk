package ca.bc.gov.app.smks.model;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import ca.bc.gov.app.smks.model.tool.About;
import ca.bc.gov.app.smks.model.tool.Scale;
import ca.bc.gov.app.smks.model.tool.Zoom;

@JsonInclude(Include.NON_NULL)
public class MapTools implements Serializable 
{
    private static final long serialVersionUID = -6479452094517424523L;

    public enum Tool { MEASURE }
	
    private boolean measure;
    private Scale scale;
    private boolean coordinate;
    private boolean minimap;
    private boolean markup;
    private boolean directions;
    private boolean pan;
    private boolean attribution;
    private Zoom zoom;
    private About about;
    private boolean baseMaps;
    private boolean layers;
    private boolean identify;
    private boolean select;

	public MapTools() { }

	protected MapTools( MapTools mapTools ) {
		this.setMeasure(mapTools.getMeasure());
		this.setScale(new Scale(mapTools.getScale()));
		this.setCoordinate(mapTools.getCoordinate());
		this.setMinimap(mapTools.getMinimap());
		this.setMarkup(mapTools.getMarkup());
		this.setDirections(mapTools.getDirections());
		this.setPan(mapTools.getPan());
		this.setAttribution(mapTools.getAttribution());
		this.setZoom(new Zoom(mapTools.getZoom()));
		this.setAbout(new About(mapTools.getAbout()));
		this.setBaseMaps(mapTools.getBaseMaps());
		this.setLayers(mapTools.getLayers());
		this.setIdentify(mapTools.getIdentify());
		this.setSelect(mapTools.getSelect());
	}

	public boolean getMeasure() { return measure; }
	public void setMeasure(boolean measure) { this.measure = measure; }

	public Scale getScale() {
		if ( scale == null ) scale = new Scale();
		return scale;
	}
	public void setScale(Scale scale) { this.scale = scale; }

	public boolean getCoordinate() { return coordinate; }
	public void setCoordinate(boolean coordinate) { this.coordinate = coordinate; }

	public boolean getMinimap() { return minimap; }
	public void setMinimap(boolean minimap) { this.minimap = minimap; }

	public boolean getMarkup() { return markup; }
	public void setMarkup(boolean markup) { this.markup = markup; }

	public boolean getDirections() { return directions; }
	public void setDirections(boolean directions) { this.directions = directions; }

	public boolean getPan() { return pan; }
	public void setPan(boolean pan) { this.pan = pan; }

	public boolean getAttribution() { return attribution; }
	public void setAttribution(boolean attribution) { this.attribution = attribution; }

	public Zoom getZoom() {
		if ( zoom == null ) zoom = new Zoom();
		return zoom;
	}
	public void setZoom(Zoom zoom) { this.zoom = zoom; }

	public About getAbout() {
		if ( about == null ) about = new About();
		return about;
	}
	public void setAbout(About about) { this.about = about; }

	public boolean getBaseMaps() { return baseMaps; }
	public void setBaseMaps(boolean baseMaps) { this.baseMaps = baseMaps; }

	public boolean getLayers() { return layers; }
	public void setLayers(boolean layers) { this.layers = layers; }

	public boolean getIdentify() { return identify; }
	public void setIdentify(boolean identify) { this.identify = identify; }

	public boolean getSelect() { return select; }
	public void setSelect(boolean select) { this.select = select; }

	public Map<String, Object> allTools() {
		try {
			return Arrays.asList(
				Introspector.getBeanInfo( this.getClass(), Object.class )
							.getPropertyDescriptors()
			)
			.stream()
			// filter out properties with setters only
			// .filter(pd -> Objects.nonNull(pd.getReadMethod()))
			.collect(Collectors.toMap(
				// bean property name
				PropertyDescriptor::getName,
				pd -> { // invoke method to get value
					try {
						return pd.getReadMethod().invoke(this);
					} catch (Exception e) {
						// replace this with better error handling
					return null;
					}
				}));
		}
		catch (IntrospectionException e) {
			// and this, too
			return Collections.emptyMap();
		}
	}
}
