/**
 *
 *  http://www.digitalekabeltelevisie.nl/dvb_inspector
 *
 *  This code is Copyright 2009-2014 by Eric Berendsen (e_berendsen@digitalekabeltelevisie.nl)
 *
 *  This file is part of DVB Inspector.
 *
 *  DVB Inspector is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  DVB Inspector is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with DVB Inspector.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  The author requests that he be notified of any application, applet, or
 *  other binary that makes use of this code, but that's more out of curiosity
 *  than anything and is not required.
 *
 */
package nl.digitalekabeltelevisie.main;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import nl.digitalekabeltelevisie.controller.ChartLabel;
import nl.digitalekabeltelevisie.controller.KVP;
import nl.digitalekabeltelevisie.controller.ViewContext;
import nl.digitalekabeltelevisie.data.mpeg.PID;
import nl.digitalekabeltelevisie.data.mpeg.TransportStream;
import nl.digitalekabeltelevisie.data.mpeg.pes.GeneralPesHandler;
import nl.digitalekabeltelevisie.gui.AboutAction;
import nl.digitalekabeltelevisie.gui.BarChart;
import nl.digitalekabeltelevisie.gui.BitRateChart;
import nl.digitalekabeltelevisie.gui.DVBtree;
import nl.digitalekabeltelevisie.gui.EITView;
import nl.digitalekabeltelevisie.gui.EnableTSPacketsAction;
import nl.digitalekabeltelevisie.gui.FileOpenAction;
import nl.digitalekabeltelevisie.gui.GridView;
import nl.digitalekabeltelevisie.gui.PIDDialog;
import nl.digitalekabeltelevisie.gui.PIDDialogOpenAction;
import nl.digitalekabeltelevisie.gui.SetPrivateDataSpecifierAction;
import nl.digitalekabeltelevisie.gui.TimeStampChart;
import nl.digitalekabeltelevisie.gui.ToggleViewAction;
import nl.digitalekabeltelevisie.gui.TransportStreamView;
import nl.digitalekabeltelevisie.gui.exception.NotAnMPEGFileException;
import nl.digitalekabeltelevisie.util.Utils;

import org.jfree.chart.plot.DefaultDrawingSupplier;

/**
 * Main class for DVB Inspector, creates and holds all GUI elements.
 *
 * @author Eric
 *
 */
public class DVBinspector implements ChangeListener, ActionListener{

	private static final Logger LOGGER = Logger.getLogger(DVBinspector.class.getName());


	/**
	 * key for storage of last used DEFAULT_PRIVATE_DATA_SPECIFIER in Preferences
	 */
	public static final String DEFAULT_PRIVATE_DATA_SPECIFIER = "private_data_spcifier";
	public static final String ENABLE_TS_PACKETS = "enable_ts_packets";

	/**
	 * key for storage of last used DEFAULT_VIEW_MODUS in Preferences
	 */
	public static final String DEFAULT_VIEW_MODUS = "view_modus";


	public static final String WINDOW_WIDTH = "window_width";
	public static final String WINDOW_HEIGHT = "window_height";
	public static final String WINDOW_X = "window_x";
	public static final String WINDOW_Y = "window_y";

	private TransportStream transportStream;

	private JFrame frame;

	private final List<TransportStreamView> views = new ArrayList<TransportStreamView>();
	private DVBtree treeView;
	private TimeStampChart timeStampChart;
	private BitRateChart bitRateView;
	private BarChart barChart;
	private GridView gridView;
	private JTabbedPane tabbedPane;
	private JMenu viewTreeMenu;
	private JMenu viewMenu;
	private PIDDialog pidDialog = null;

	private long defaultPrivateDataSpecifier = 0;
	private boolean enableTSPackets = false;

	private ViewContext viewContext = new ViewContext();

	private int modus;



	/**
	 * Default constructor for DVBinspector
	 */
	public DVBinspector() {
		super();
	}

	/**
	 * Starting point for DVB Inspector
	 *
	 * @param args String[] all optional, if used; arg[0] is absolute filename of transport stream to be loaded on startup.
	 *  args[1...n] pids with PES data that should be parsed on startup (equivalent to "Parse PES data" menu in Tree View)
	 *  These args are mainly intended for debugging, where you want to use the same stream and PES data over and over again.
	 *
	 */
	public static void main(final String[] args) {
		final DVBinspector inspector = new DVBinspector();
		if(args.length>=1){
			final String filename= args[0];
			try {
				final TransportStream ts = new TransportStream(filename);

				final Preferences prefs = Preferences.userNodeForPackage(DVBinspector.class);

				ts.setDefaultPrivateDataSpecifier(prefs.getLong(DVBinspector.DEFAULT_PRIVATE_DATA_SPECIFIER, 0));
				ts.setEnableTSPackets(prefs.getBoolean(DVBinspector.ENABLE_TS_PACKETS, false));

				inspector.transportStream = ts;

				inspector.transportStream.parseStream();
				if(args.length>=2){

					final PID[] pids = ts.getPids();
					final Map<Integer, GeneralPesHandler> pesHandlerMap = new HashMap<Integer, GeneralPesHandler>();
					for (int i = 1; i < args.length; i++) {
						final int pid=Integer.parseInt(args[i]);
						final PID p= pids[pid];
			            if (p != null) {
			                pesHandlerMap.put(Integer.valueOf(p.getPid()), p.getPesHandler());
			            }
			        }
			        ts.parseStream(null,pesHandlerMap);
				}
			} catch (final NotAnMPEGFileException e) {
				LOGGER.log(Level.WARNING, "error determining packetsize transportStream", e);
			} catch (final IOException e) {
				LOGGER.log(Level.WARNING, "error parsing transportStream", e);
			}
		}
		inspector.run();
	}

	public void run() {
		final Preferences prefs = Preferences.userNodeForPackage(DVBinspector.class);
		defaultPrivateDataSpecifier = prefs.getLong(DVBinspector.DEFAULT_PRIVATE_DATA_SPECIFIER, 0);
		enableTSPackets = prefs.getBoolean(DVBinspector.ENABLE_TS_PACKETS, false);
		modus = prefs.getInt(DVBinspector.DEFAULT_VIEW_MODUS,0);

		KVP.setNumberDisplay(KVP.NUMBER_DISPLAY_BOTH);
		KVP.setStringDisplay(KVP.STRING_DISPLAY_HTML_AWT);
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI(transportStream,modus);
			}
		});

	}

	/**
	 * Create the GUI and show it.  For thread safety,
	 * this method should be invoked from the
	 * event dispatch thread.
	 *
	 * @param tStream Transport stream (can be <code>null</code>)
	 * @param modus display Modus
	 */
	private void createAndShowGUI(final TransportStream tStream,final int modus) {
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (final Exception e)
		{
			LOGGER.warning("Couldn't use system look and feel.");
		}

		ToolTipManager.sharedInstance().setDismissDelay(30000);
		//Create and set up the window.
		frame = new JFrame("DVB Inspector");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			/* (non-Javadoc)
			 * @see java.awt.event.WindowAdapter#windowClosing(java.awt.event.WindowEvent)
			 */
			@Override
			public void windowClosing(final WindowEvent e) {
				saveWindowState();
				super.windowClosing(e);
			}
		});
		pidDialog= new PIDDialog(frame,viewContext,this);
		updatePIDLists(tStream,pidDialog);

		tabbedPane = new JTabbedPane();
		treeView = new DVBtree(tStream,modus);
		tabbedPane.addTab("Tree", treeView);
		views.add(treeView);

		final EITView eitView = new EITView(tStream,viewContext);
		tabbedPane.addTab("EIT View", eitView);
		views.add(eitView);

		bitRateView = new BitRateChart(tStream,viewContext);
		tabbedPane.addTab("BitRate View", bitRateView);
		views.add(bitRateView);

		barChart = new BarChart(tStream,viewContext);
		tabbedPane.addTab("Bar View", barChart);
		views.add(barChart);

		gridView = new GridView(tStream,viewContext);
		tabbedPane.addTab("Grid View", gridView);
		views.add(gridView);

		timeStampChart = new TimeStampChart(tStream, viewContext);
		tabbedPane.addTab("PCR/PTS/DTS View", timeStampChart);
		views.add(timeStampChart);

		tabbedPane.validate();
		tabbedPane.addChangeListener(this);
		frame.add(tabbedPane);

		frame.setJMenuBar(createMenuBar(modus));
		enableViewMenus();

		final Image image = Utils.readIconImage("magnifying_glass.bmp");
		frame.setIconImage(image);

		frame.setBounds(calculateBounds(Preferences.userNodeForPackage(DVBinspector.class)));

		frame.setVisible(true);
	}

	/**
	 * @param modus
	 * @return
	 */
	private JMenuBar createMenuBar(final int modus) {
		final JMenuBar menuBar = new JMenuBar();

		menuBar.add(createFileMenu());

		viewTreeMenu = createViewTreeMenu(modus);
		menuBar.add(viewTreeMenu);

		viewMenu = createViewMenu();
		menuBar.add(viewMenu);

		menuBar.add(createSettingsMenu());
		menuBar.add(createHelpMenu());
		return menuBar;
	}

	/**
	 * @param prefs
	 * @return
	 */
	private Rectangle calculateBounds(final Preferences prefs) {

		int x = Math.max(0, prefs.getInt(DVBinspector.WINDOW_X, 10));
		int y = Math.max(0, prefs.getInt(DVBinspector.WINDOW_Y, 10));

		// if last used on larger screen, it might open too large, or out of range
		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		final int screenWidth = (int) screenSize.getWidth();
		final int screenHeight = (int) screenSize.getHeight();

		x = Math.min(screenWidth-200, x); // at least 200 pix visible
		y = Math.min(screenHeight-200, y); // at least 200 pix visible
		final int w = Math.min(screenWidth, prefs.getInt(DVBinspector.WINDOW_WIDTH, 980));
		final int h = Math.min(screenHeight, prefs.getInt(DVBinspector.WINDOW_HEIGHT, 700));
		return new Rectangle(x,y,w,h);
	}

	/**
	 * @return
	 */
	private JMenu createSettingsMenu() {
		final JMenu settingsMenu =new JMenu("Settings");
		final JMenu privateDataSubMenu = new JMenu("Private Data Specifier Default");
		final ButtonGroup group = new ButtonGroup();

		addPrivateDataSpecMenuItem(privateDataSubMenu, group, 0x00, "none",defaultPrivateDataSpecifier);
		addPrivateDataSpecMenuItem(privateDataSubMenu, group, 0x16, "Casema",defaultPrivateDataSpecifier);
		addPrivateDataSpecMenuItem(privateDataSubMenu, group, 0x28, "EACEM",defaultPrivateDataSpecifier);
		addPrivateDataSpecMenuItem(privateDataSubMenu, group, 0x29, "Nordig",defaultPrivateDataSpecifier);
		addPrivateDataSpecMenuItem(privateDataSubMenu, group, 0x40, "CI Plus LLP",defaultPrivateDataSpecifier);
		addPrivateDataSpecMenuItem(privateDataSubMenu, group, 0x600, "UPC",defaultPrivateDataSpecifier);
		addPrivateDataSpecMenuItem(privateDataSubMenu, group, 0x0000233A, "Independent Television Commission (DTG)",defaultPrivateDataSpecifier);

		settingsMenu.add(privateDataSubMenu);

		final JCheckBoxMenuItem enableTSPacketsMenu = new JCheckBoxMenuItem("Enable TS Packets");
		enableTSPacketsMenu.setSelected(enableTSPackets);
		final Action enableTSPacketsAction= new EnableTSPacketsAction(this);
		enableTSPacketsMenu.addActionListener(enableTSPacketsAction);
		settingsMenu.add(enableTSPacketsMenu);
		return settingsMenu;
	}

	/**
	 * @return
	 */
	private JMenu createViewMenu() {
		final JMenu viewMenu =new JMenu("View");
		final JMenuItem filterItem = new JMenuItem("Filter");
		viewMenu.add(filterItem);
		final Action pidOpenAction = new PIDDialogOpenAction(pidDialog,frame,this);
		filterItem.addActionListener(pidOpenAction);
		viewMenu.add(filterItem);
		return viewMenu;
	}

	/**
	 * @return
	 */
	private JMenu createHelpMenu() {
		final JMenu helpMenu =new JMenu("Help");
		JMenuItem aboutMenuItem;
		aboutMenuItem = new JMenuItem("About...");
		final Action aboutAction= new AboutAction(new JDialog(), frame,this);
		aboutMenuItem.addActionListener(aboutAction);
		helpMenu.add(aboutMenuItem);
		return helpMenu;
	}

	/**
	 * @param modus
	 */
	private JMenu createViewTreeMenu(final int modus) {
		final JMenu viewTreeMenu =new JMenu("Tree View");
		viewTreeMenu.add(createCheckBoxMenuItem(modus, "Simple Tree View", DVBtree.SIMPLE_MODUS));
		viewTreeMenu.add(createCheckBoxMenuItem(modus, "PSI Only",DVBtree.PSI_ONLY_MODUS));
		viewTreeMenu.add(createCheckBoxMenuItem(modus, "Packet Count",DVBtree.PACKET_MODUS));
		viewTreeMenu.add(createCheckBoxMenuItem(modus, "Number List Items",DVBtree.COUNT_LIST_ITEMS_MODUS));
		viewTreeMenu.add(createCheckBoxMenuItem(modus, "Show PTS on PES Packets",DVBtree.SHOW_PTS_MODUS));
		viewTreeMenu.add(createCheckBoxMenuItem(modus, "Show version_number on Table Sections",DVBtree.SHOW_VERSION_MODUS));

		return viewTreeMenu;
	}

	/**
	 * @param modus
	 * @param label
	 * @param modusBit
	 * @return
	 */
	private JCheckBoxMenuItem createCheckBoxMenuItem(final int modus, final String label, final int modusBit) {
		final JCheckBoxMenuItem viewMenu = new JCheckBoxMenuItem(label);
		viewMenu.setSelected((modus&modusBit)!=0);
		final Action viewAction= new ToggleViewAction(this, modusBit);
		viewMenu.addActionListener(viewAction);
		return viewMenu;
	}

	/**
	 * @return
	 */
	private JMenu createFileMenu() {
		final JMenu fileMenu =new JMenu("File");
		final JMenuItem openMenuItem = new JMenuItem("Open",
				KeyEvent.VK_O);

//		exportMenuItem = new JMenuItem("Export as HTML");
//		exportMenuItem.setEnabled(false);
		final JMenuItem exitMenuItem = new JMenuItem("Exit");

		final Action fileOpenAction= new FileOpenAction(new JFileChooser(),frame,this);
		openMenuItem.addActionListener(fileOpenAction);
		fileMenu.add(openMenuItem);

//		final Action exportAction= new ExportAction(frame,this);
//		fileMenu.add(exportMenuItem);
//		exportMenuItem.addActionListener(exportAction);

		exitMenuItem.addActionListener(this);
		fileMenu.add(exitMenuItem);
		return fileMenu;
	}


	/**
	 * Add a menu item for a private data specifier to the "Private Data Specifier Default" menu
	 *
	 * @param privateDataSubMenu
	 * @param group
	 * @param spec
	 * @param name
	 * @param defaultSpecifier
	 */
	private void addPrivateDataSpecMenuItem(final JMenu privateDataSubMenu, final ButtonGroup group, final long spec, final String name, final long defaultSpecifier) {
		final JMenuItem menuItem = new JRadioButtonMenuItem(Utils.toHexString(spec, 8)+" - "+name);
		group.add(menuItem);
		menuItem.addActionListener(new SetPrivateDataSpecifierAction(this,spec));
		menuItem.setSelected(spec==defaultSpecifier);
		privateDataSubMenu.add(menuItem);
	}

	/**
	 * getter for the Transport stream (can be <code>null</code>)
	 * @return
	 */
	public TransportStream getTransportStream() {
		return transportStream;
	}

	/**
	 * setter for the Transport stream (can be <code>null</code>)
	 * @param transportStream
	 */
	public void setTransportStream(final TransportStream transportStream) {
		this.transportStream = transportStream;
		if(transportStream!=null){
			updatePIDLists(transportStream,pidDialog);
			frame.setTitle(transportStream.getFile().getName()+ " - stream:"+ transportStream.getStreamID()+ " - DVB Inspector");
		}

		for(final TransportStreamView v: views) {
			v.setTransportStream(transportStream,viewContext);
		}
		enableViewMenus();

	}

	public DVBtree getTreeView() {
		return treeView;
	}

	public void setTreeView(final DVBtree treeView) {
		this.treeView = treeView;
	}



	/**
	 * Update the list of all pids when a new stream is loaded, used in the {@link PIDDialog}
	 * @param tStream
	 * @param pDialog
	 */
	private void updatePIDLists(final TransportStream tStream, final PIDDialog pDialog){

		final ViewContext viewConfig = new ViewContext();
		final ArrayList<ChartLabel> used = new ArrayList<ChartLabel>();
		final ArrayList<ChartLabel> notUsed = new ArrayList<ChartLabel>();

		if(tStream!=null){
			final short[] used_pids=tStream.getUsedPids();
			for (int i = 0; i < used_pids.length; i++) {
				final short actualPid = used_pids[i];
				used.add(new ChartLabel(actualPid+" - "+transportStream.getShortLabel(actualPid),actualPid, DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE[i%DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE.length]));
			}
			viewConfig.setStartPacket(0);
			viewConfig.setEndPacket(tStream.getNo_packets());
			viewConfig.setMaxPacket(tStream.getNo_packets());
		}

		viewConfig.setShown(used);
		viewConfig.setNotShown(notUsed);
		viewConfig.setTransportStream(tStream);
		viewContext = viewConfig;
		pDialog.setConfig(viewConfig);
	}



	/**
	 * Called by PIDDialog, when the list of PIDs to be shown has changed. Forwards new list to all views that need it.
	 * @param vContext
	 */
	public void setPIDList(final ViewContext vContext){
		viewContext = vContext;
		if(transportStream!=null){
			bitRateView.setTransportStream(transportStream, vContext);
			barChart.setTransportStream(transportStream, vContext);
			gridView.setTransportStream(transportStream, vContext);
			timeStampChart.setTransportStream(transportStream, vContext);
			//ignore eitView on this, because it always uses only one PID.
		}
	}

	/**
	 * @return the defaultPrivateDataSpecifier
	 */
	public long getDefaultPrivateDataSpecifier() {
		return defaultPrivateDataSpecifier;
	}

	/**
	 * @param defaultPrivateDataSpecifier the defaultPrivateDataSpecifier to set
	 */
	public void setDefaultPrivateDataSpecifier(final long defaultPrivateDataSpecifier) {
		this.defaultPrivateDataSpecifier = defaultPrivateDataSpecifier;
	}

	/**
	 * Listen for changes caused by selecting another tab from tabbedPane. Then enable/disable appropriate menu's.
	 *
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */

	public void stateChanged(final ChangeEvent e) {
		enableViewMenus();
	}

	/**
	 * enables menus based on which view is selected in the tabbedPane
	 */
	private void enableViewMenus() {
		final int i = tabbedPane.getSelectedIndex();
		viewTreeMenu.setEnabled((i==0)&&(transportStream!=null));
		viewMenu.setEnabled((i>1)&&(transportStream!=null));
//		exportMenuItem.setEnabled(transportStream!=null);
	}


	/**
	 * store current window position and size in preferences
	 */
	private void saveWindowState(){
		final Preferences prefs = Preferences.userNodeForPackage(DVBinspector.class);
		prefs.putInt(WINDOW_WIDTH,frame.getWidth());
		prefs.putInt(WINDOW_HEIGHT,frame.getHeight());
		prefs.putInt(WINDOW_X,frame.getX());
		prefs.putInt(WINDOW_Y,frame.getY());

	}

	/**
	 * called when "exit" is selected in menu.
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(final ActionEvent e) {
		saveWindowState();
		System.exit(0);
	}

	public JFrame getFrame() {
		return frame;
	}

	public int getModus() {
		return modus;
	}

	public void setModus(final int modus) {
		this.modus = modus;
	}

	public boolean isEnableTSPackets() {
		return enableTSPackets;
	}

	public void setEnableTSPackets(final boolean enableTSPackets) {
		this.enableTSPackets = enableTSPackets;
	}


}