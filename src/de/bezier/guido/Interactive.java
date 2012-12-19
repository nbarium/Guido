package de.bezier.guido;

import processing.core.*;
import java.util.*;
import java.awt.event.*;
import java.lang.reflect.*;

/**
 *	<h1>This is the main element for you to use.</h1>
 *
 *	<p>
 *	The normal setup would be to create a class implementing some of the callbacks
 *  provided by Guido and then register it with the manager.
 *  </p>
 *
 *  <p>
 *	See <a href="ReflectiveActiveElement.html">ReflectiveActiveElement</a> for a list 
 *	of all available callbacks and their forms.
 *	</p>
 */
public class Interactive 
implements MouseWheelListener
{
	private boolean enabled = true;

	ArrayList<AbstractActiveElement> interActiveElements;
	static Interactive manager;
	PApplet papplet;

	static HashMap<String, ArrayList<EventBinding>> eventBindings;
	static Method emitMethod;
	
	private Interactive ( PApplet papplet )
	{
		this.papplet = papplet;
		
		papplet.registerMouseEvent( this );

		if ( papplet.frame != null )
		{
			papplet.frame.addMouseWheelListener(this);
		}
		
		papplet.registerDraw( this );
	}
	
	/**
	 *	Main entry point for PApplet (from your sketch).
	 *
	 *	@param papplet Your sketch
	 */
	public static Interactive make ( PApplet papplet )
	{
		if ( manager == null )
			manager = new Interactive( papplet );

		return manager;
	}
	
	/**
	 *	Get the actual manager instance.
	 *
	 *	@return the Interactive instance which is the actual manager, or null
	 */
	public static Interactive get ()
	{
		if ( manager == null )
		{
			System.err.println( "You need to initialize me first with ...\n\n\tInteractive.make(this);\n" );
		}
		return manager;
	}

	// ------------------------------------------
	//	static methods, states, utils
	// ------------------------------------------

	/**
	 *	Activate or deactivate and interface element.
	 *
	 *	@param element  the interface element
	 *	@param state	the state: true for active, false to deactivate
	 */
	public static void setActive ( Object element, boolean state )
	{
		if ( manager != null )
		{
			for ( AbstractActiveElement interActiveElement : manager.interActiveElements )
			{
				if ( interActiveElement.getClass().equals( ReflectiveActiveElement.class ) )
				{
					if ( ((ReflectiveActiveElement)interActiveElement).listener == element )
					{
						interActiveElement.setActive( state );
					}
				}
				else if ( interActiveElement == element )
				{	
					interActiveElement.setActive( state );
				}
			}
		}
	}

	/**
	 *	A utility function to do a simple box test.
	 *
	 *	@param	x 		left coordinate
	 *	@param	y		top coordinate
	 *	@param	width 	width of the rectangle
	 *	@param	height 	height of the rectangle
	 *	@param	mx		x of point to test
	 *	@param	my		y of point to test
	 *  @return 		true if the point is inside the rectangle
	 */
	public static boolean insideRect ( float x, float y, float width, float height, float mx, float my )
	{
		return mx >= x && mx <= x+width && my >= y && my <= y+height;
	}

	/**
	 *	Add an element to the manager to be managed.
	 *
	 *	<p>The easiest way to use this is inside your elements constructor:
	 *  <code>
	 *	public class YourElement {
	 *		YourElement () {
	 *			Interactive.add( this );
	 *		}
	 *	}
	 *  </code></p>
	 *
	 *	@param element the element to manage
	 */
	public static void add ( Object element )
	{
		if ( manager == null) {
			System.err.println( "You need to call Interactive.make() first." );
			return;
		}

		Class klass = element.getClass();
		while ( klass != null ) {
			if ( klass == AbstractActiveElement.class ) return;
			klass = klass.getSuperclass();
		}

		// this adds itself to Interactive in constructor
		new ReflectiveActiveElement( element );
	}

	/**
	 *	Trying to set a field in an object to a given value.
	 */
	public static void set ( Object obj, String fieldName, Object value ) 
	{
		if ( obj == null || fieldName == null || value == null )
		{
			System.err.println( "Interactive.set() ... a value is null!" );
		}
		Field field = Interactive.getField( obj, fieldName, value );
		if ( field != null )
		{
			try {
	            field.set( obj, value );
	        } catch ( Exception e ) {
	            e.printStackTrace();
	        }
		}
	}

	/**
	 *	Get a field from an object by looking at the fields name and value signature.
	 */
	public static Field getField ( Object obj, String fieldName, Object value )
	{
		Class valueClass = value.getClass();

		Field[] fields = obj.getClass().getDeclaredFields();
		for ( Field f : fields ) 
		{	
			if ( f.getType().isAssignableFrom( valueClass ) && f.getName().equals( fieldName ) )
			{
				return f;
			}
		}

		// what to do here?

		System.err.println( "Interactive.set() ... unable to find a field with that name in given target" );
		return null;
	}

	public static void on ( String eventName, Object targetObject, String targetMethodName )
	{
		Interactive.on( null, eventName, targetObject, targetMethodName );
	}

	public static void on ( Object emitterObject, String eventName, Object targetObject, String targetMethodName )
	{
		Method targetMethod = null;
		try {
			Method[] meths = targetObject.getClass().getDeclaredMethods();
			for ( Method m : meths ) 
			{
				if ( m.getName().equals( targetMethodName ) )
				{
					targetMethod = m;
				}
			}
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		if ( targetMethod != null )
		{
			String bindingId = EventBinding.getIdFor( emitterObject, eventName );
			EventBinding binding = new EventBinding( eventName, targetObject, targetMethod );
			if ( eventBindings == null ) eventBindings = new HashMap();
			ArrayList<EventBinding> list = eventBindings.get( bindingId );
			if ( list == null )
			{
				list = new ArrayList();
				eventBindings.put( bindingId, list );
			}
			list.add( binding );
		}
		else
		{
			System.err.println( "Sorry, that method named " + targetMethodName + " could not be found ... " );
		}
	}

	public static void send ( String eventName, Object...eventValues )
	{
		if ( emitMethod == null ) {
			try {
				emitMethod = Interactive.class.getDeclaredMethod( "send", new Class[]{
					Object.class, String.class, Object[].class
				});
			} catch ( Exception e ) {
				e.printStackTrace();
			}
		}
		try {
			emitMethod.invoke( null, new Object[]{ null, eventName, eventValues } );
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}

	public static void send ( Object emitterObject, String eventName, Object...eventValues )
	{
		if ( eventBindings == null )
		{
			System.err.println( "No bindings exist at the moment" );
			return;
		}
		String bindingId = EventBinding.getIdFor( emitterObject, eventName );
		ArrayList<EventBinding> list = eventBindings.get( bindingId );
		if ( list != null )
		{
			for ( EventBinding binding : list )
			{
				if ( binding != null )
				{
					binding.send( eventValues );
				}
			}
		}
	}
	
	public static boolean isActive ()
	{
		if ( manager != null ) {
			return manager.isEnabled();
		}
		return false;
	}

	public static void toggle ()
	{
		if ( manager != null ) {
			manager.setEnabled( manager.isEnabled() ? false : true );
		}
	}

	public static void deactivate ()
	{
		if ( manager != null ) {
			manager.setEnabled( false );
		}
	}

	public static void activate ()
	{
		if ( manager != null ) {
			manager.setEnabled( true );
		}
	}

	public static void setActive ( boolean tf )
	{
		if ( manager != null ) {
			manager.setEnabled( tf );
		}
	}
	
	// ------------------------------------------
	//	instance methods
	// ------------------------------------------


	public boolean isEnabled ()
	{
		return enabled;
	}

	public void setEnabled ( boolean tf )
	{
		enabled = tf;
	}
	
	/**
	 *	Callback for Component.addMouseWheelListener()
	 *
	 *	@param e the mouse wheel (scroll) event
	 *  @see java.awt.Component#addMouseWheelListener(java.awt.event.MouseWheelListener)
	 */
	public void mouseWheelMoved ( MouseWheelEvent e ) 
	{
		if ( !enabled ) return;

		int step = e.getWheelRotation();
		for ( AbstractActiveElement interActiveElement : interActiveElements )
		{
			if ( !interActiveElement.isActive() ) continue;

			interActiveElement.mouseScrolled( step );
			
			float mx = papplet.mouseX;
			float my = papplet.mouseY;
			boolean wasHover = interActiveElement.hover;
			interActiveElement.hover = interActiveElement.isInside( mx, my );
			if ( interActiveElement.hover && !wasHover )
			{
				interActiveElement.mouseEntered( );
				interActiveElement.mouseEntered( mx, my );
			}
			else if ( !interActiveElement.hover && wasHover )
			{
				interActiveElement.mouseExited( );
				interActiveElement.mouseExited( mx, my );
			}
		}
	}
	
	/**
	 *	Add an element to the manager, mainly used internally.
	 *
	 *	@param activeElement the element to manage
	 */
	public void addElement ( AbstractActiveElement activeElement )
	{
		if ( interActiveElements == null ) 
			interActiveElements = new ArrayList<AbstractActiveElement>();
		if ( !interActiveElements.contains(activeElement) )
			interActiveElements.add( activeElement );
	}
	
	/**
	 *	Callback for PApplet.registerDraw()
	 *
	 *	@see <a href="http://processing.googlecode.com/svn/trunk/processing/build/javadoc/core/processing/core/PApplet.html#registerDraw(java.lang.Object)">PApplet.registerDraw( Object obj )</a>
	 */
	public void draw ()
	{
		if ( !enabled ) return;
		
		if ( interActiveElements == null ) return;
		
		for ( AbstractActiveElement interActiveElement : interActiveElements )
		{
			if ( !interActiveElement.isActive() ) continue;
			interActiveElement.draw();
		}
	}
	
	/**
	 *	Callback for PApplet.registerMouseEvent()
	 *
	 *	@see <a href="http://processing.googlecode.com/svn/trunk/processing/build/javadoc/core/processing/core/PApplet.html#registerMouseEvent(java.lang.Object)">PApplet.registerMouseEvent(Object obj)</a>
	 */
	public void mouseEvent ( MouseEvent evt )
	{
		if ( !enabled ) return;
		
		if ( interActiveElements == null ) return;
		
		switch ( evt.getID() )
		{
			case MouseEvent.MOUSE_ENTERED:
				mouseEntered( evt );
				break;
			case MouseEvent.MOUSE_MOVED:
				mouseMoved( evt );
				break;
			case MouseEvent.MOUSE_PRESSED:
				mousePressed( evt );
				break;
			case MouseEvent.MOUSE_DRAGGED:
				mouseDragged( evt );
				break;
			case MouseEvent.MOUSE_RELEASED:
				mouseReleased( evt );
				break;
			case MouseEvent.MOUSE_CLICKED:
				//mousePressed( evt );
				break;
			case MouseEvent.MOUSE_EXITED:
				mouseExited( evt );
				break;
				
		}
	}
	
	private void mouseEntered ( MouseEvent evt ) {
	}
	
	private void mouseMoved ( MouseEvent evt )
	{
		int mx = evt.getX();
		int my = evt.getY();

		for ( AbstractActiveElement interActiveElement : interActiveElements )
		{
			if ( !interActiveElement.isActive() ) continue;
			
			boolean wasHover = interActiveElement.hover;
			interActiveElement.hover = interActiveElement.isInside( mx, my );
			if ( interActiveElement.hover && !wasHover )
			{
				interActiveElement.mouseEntered( );
				interActiveElement.mouseEntered( mx, my );
			}
			else if ( !interActiveElement.hover && wasHover )
			{
				interActiveElement.mouseExited( );
				interActiveElement.mouseExited( mx, my );
			}
			else
			{
				interActiveElement.mouseMoved( );
				interActiveElement.mouseMoved( mx, my );
			}
		}
	}
	
	private void mousePressed ( MouseEvent evt )
	{
		int mx = evt.getX();
		int my = evt.getY();
		
		for ( AbstractActiveElement interActiveElement : interActiveElements )
		{
			if ( !interActiveElement.isActive() ) continue;

			if ( interActiveElement.hover )
				interActiveElement.mousePressedPre( mx, my );
		}
	}
	
	private void mouseDragged ( MouseEvent evt )
	{
		int mx = evt.getX();
		int my = evt.getY();
		
		for ( AbstractActiveElement interActiveElement : interActiveElements )
		{
			if ( !interActiveElement.isActive() ) continue;

			if ( interActiveElement.hover )
				interActiveElement.mouseDraggedPre( mx, my );
		}
	}
	
	private void mouseReleased ( MouseEvent evt )
	{
		int mx = evt.getX();
		int my = evt.getY();
		
		for ( AbstractActiveElement interActiveElement : interActiveElements )
		{
			if ( !interActiveElement.isActive() ) continue;

			if ( !interActiveElement.hover ) continue;
			
			interActiveElement.mouseReleasedPre( mx, my );
			interActiveElement.mouseReleasedPost( mx, my );
		}
	}
	
	private void mouseExited ( MouseEvent evt ) {
	}
}