/*
 * Author: atotic
 * Created on May 6, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;

import java.io.File;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Provides decoration for model elements in the debugger interface.
 */
public class PyDebugModelPresentation implements IDebugModelPresentation {

	static public String PY_DEBUG_MODEL_ID = "org.python.pydev.debug";

	protected ListenerList fListeners = new ListenerList(ListenerList.IDENTITY);

	protected boolean displayVariableTypeNames = false; // variables display attribute

    /**
     * @return the image for some debug element
     */
	public Image getImage(Object element) {
		if (element instanceof PyBreakpoint) {
			try {
				PyBreakpoint pyBreakpoint = (PyBreakpoint) element;
				
				if ((pyBreakpoint).isEnabled())
					if (pyBreakpoint.isConditionEnabled()){
						return PydevDebugPlugin.getImageCache().get("icons/breakmarker_conditional.gif");
					}else{
						return PydevDebugPlugin.getImageCache().get("icons/breakmarker.gif");
					}
				
				else if (pyBreakpoint.isConditionEnabled()){
					return PydevDebugPlugin.getImageCache().get("icons/breakmarker_gray_conditional.gif");
				}else{
					return PydevDebugPlugin.getImageCache().get("icons/breakmarker_gray.gif");
				}
				
			} catch (CoreException e) {
				PydevDebugPlugin.log(IStatus.ERROR, "getImage error", e);
			}
			
		} else if (element instanceof PyVariableCollection) {
			return PydevDebugPlugin.getImageCache().get("icons/greendot_big.gif");
			
		} else if (element instanceof PyVariable) {
			return PydevDebugPlugin.getImageCache().get("icons/greendot.gif");
			
		} else if (element instanceof PyDebugTarget || element instanceof PyThread || element instanceof PyStackFrame){
			return null;
		}
		
		return null;
	}

    /**
     * @return the text for some debug element
     */
	public String getText(Object element) {
		if (element instanceof PyBreakpoint) {
			PyBreakpoint pyBreakpoint = (PyBreakpoint) element;
            IMarker marker = ((PyBreakpoint) element).getMarker();
			try {
				Map attrs = marker.getAttributes();
                
                //get the filename
				String ioFile = pyBreakpoint.getFile();
				String fileName = "unknown";
                if(ioFile != null){
                    File file = new File(ioFile);
                    fileName = file.getName();
                }
                
                
                //get the line number
				Object lineNumber = attrs.get(IMarker.LINE_NUMBER);
				String functionName = (String) attrs.get(PyBreakpoint.FUNCTION_NAME_PROP);
				
				if (lineNumber == null){
					lineNumber = "unknown";
				}
				
                //get the location
				String location = fileName + ":" + lineNumber.toString();
				if (functionName == null){
					return location;
				}else{
					return functionName + " [" + location + "]";
				}
				
			} catch (CoreException e) {
				PydevDebugPlugin.log(IStatus.ERROR, "error retreiving marker attributes", e);
				return "error";
			}
		} else if (element instanceof AbstractDebugTarget || element instanceof PyStackFrame || element instanceof PyThread) {
			return null; // defaults work
			
		} else if (element instanceof PyVariableCollection || element instanceof PyVariable) {
			return null; // defaults are fine
			
		} else if (element instanceof IWatchExpression) {
			try {
				IWatchExpression watch_expression = (IWatchExpression) element;
				IValue value = watch_expression.getValue();
				if (value != null) {
					return "\"" + watch_expression.getExpressionText() + "\"= " + value.getValueString();
				} else {
					return null;
				}
			} catch (DebugException e) {
				return null;
			}
            
		}else if(element == null){
		    PydevDebugPlugin.log(IStatus.ERROR, "PyDebugModelPresentation: element == null", null);
            return null;
            
        }else{
            PydevDebugPlugin.log(IStatus.ERROR, "PyDebugModelPresentation:\nclass not expected for presentation:"+element.getClass()+"\n(returning default presentation).", null);
            return null;
        }
	}

	/**
	 * We've got some work to do to replicate here, because we can't return null, and have LazyModel presentation do the
	 * default
	 */
	public void computeDetail(IValue value, IValueDetailListener listener) {
		if (value instanceof PyVariable) {
			try {
				((PyVariable) value).getVariables();
				listener.detailComputed(value, ((PyVariable) value).getDetailText());
			} catch (DebugException e) {
				PydevDebugPlugin.errorDialog("Unexpected error fetching variable", e);
			}
		}
	}

	/**
	 * Returns editor to be displayed
	 */
	public IEditorInput getEditorInput(Object element) {
		if (element instanceof PyBreakpoint) {
			String file = ((PyBreakpoint) element).getFile();
            if(file != null){
    			IPath path = new Path(file);
    			IEditorPart part = PydevPlugin.doOpenEditor(path, false);
    			return part.getEditorInput();
            }
		}
		return null;
	}

	/**
	 * @see org.eclipse.debug.ui.ISourcePresentation#getEditorInput
	 */
	public String getEditorId(IEditorInput input, Object element) {
		return null;
	}

	public void setAttribute(String attribute, Object value) {
		if (attribute.equals(IDebugModelPresentation.DISPLAY_VARIABLE_TYPE_NAMES))
			displayVariableTypeNames = ((Boolean) value).booleanValue();
		else
			System.err.println("setattribute");
	}

	public void addListener(ILabelProviderListener listener) {
		fListeners.add(listener);
	}

	public void removeListener(ILabelProviderListener listener) {
		fListeners.remove(listener);
	}

	public void dispose() {
	}

	public boolean isLabelProperty(Object element, String property) {
		// Not really sure what this does. see IBaseLabelProvider:isLabelProperty
		return false;
	}
}
