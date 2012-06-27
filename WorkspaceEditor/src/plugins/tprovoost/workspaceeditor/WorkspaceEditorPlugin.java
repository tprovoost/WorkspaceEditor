package plugins.tprovoost.workspaceeditor;

import icy.common.Version;
import icy.file.FileUtil;
import icy.gui.component.ComponentUtil;
import icy.gui.component.IcyLogo;
import icy.gui.component.IcyTextField;
import icy.gui.component.IcyTextField.TextChangeListener;
import icy.gui.component.PopupPanel;
import icy.gui.component.button.IcyButton;
import icy.gui.dialog.ConfirmDialog;
import icy.gui.dialog.MessageDialog;
import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.main.Icy;
import icy.plugin.PluginDescriptor;
import icy.plugin.PluginLoader;
import icy.plugin.abstract_.PluginActionable;
import icy.preferences.WorkspaceLocalPreferences;
import icy.resource.icon.IcyIcon;
import icy.util.EventUtil;
import icy.workspace.Workspace;
import icy.workspace.Workspace.TaskDefinition;
import icy.workspace.Workspace.TaskDefinition.BandDefinition;
import icy.workspace.Workspace.TaskDefinition.BandDefinition.ItemDefinition;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.pushingpixels.flamingo.api.ribbon.RibbonElementPriority;
import org.pushingpixels.substance.api.ComponentState;
import org.pushingpixels.substance.api.SubstanceColorScheme;
import org.pushingpixels.substance.internal.utils.SubstanceColorSchemeUtilities;

public class WorkspaceEditorPlugin extends PluginActionable {

	// -----
	// GUI
	// -----
	private JPanel panelPluginsCenter = new JPanel();

	/** Panel containing the combo boxes and the {@link PopupPanel}s. */
	private JPanel panelWorkspace = new JPanel();
	/** Contains the method for the drag'n drop */
	private MainPanel mainPanel = new MainPanel();

	/** Current workspace in use. */
	private Workspace currentWorkspace;

	/** Reference to the IcyFrame. */
	private IcyFrame frame;

	// PREFERENCES
	// private XMLPreferences prefs = IcyPreferences.pluginRoot(this);

	/** Used for the drag'n drop: contains all the selected plugins. */
	private ArrayList<PluginDescriptorButton> _selectedPlugins = new ArrayList<WorkspaceEditorPlugin.PluginDescriptorButton>();
	private ArrayList<PluginDescriptorButton> listInstalledPlugins = new ArrayList<PluginDescriptorButton>();
	private String minNamePlugin = "";

	// -----------------
	// IMAGES RESSOURCES
	// -----------------
	private ImageIcon smallButton = new ImageIcon(getImageResource("plugins/tprovoost/workspaceeditor/images/sqBtnSmall.jpg"));
	private ImageIcon mediumButton = new ImageIcon(getImageResource("plugins/tprovoost/workspaceeditor/images/sqBtnMedium.jpg"));
	private ImageIcon largeButton = new ImageIcon(getImageResource("plugins/tprovoost/workspaceeditor/images/sqBtnLarge.jpg"));
	private Image minusWorkspace = getImageResource("plugins/tprovoost/workspaceeditor/images/minusWorkspace.png");
	private Image plusBand = getImageResource("plugins/tprovoost/workspaceeditor/images/plusBand.png");
	private Image plusWorkspace = getImageResource("plugins/tprovoost/workspaceeditor/images/plusWorkspace.png");
	private Image imgHelp = getImageResource("plugins/tprovoost/workspaceeditor/images/WE_Help.png");

	private JSplitPane splitPane;

	private JButton btnProperties = new JButton("Properties");
	private TextFieldFilter tfFilter;

	@Override
	public void run() {
		if (!Icy.version.isGreaterOrEqual(new Version("1.1.4.0"))) {
			MessageDialog.showDialog("Icy Version", "This plugin must be run under version 1.1.4.0 at least.");
			return;
		}

		// -------------
		// PANEL PLUGINS
		// -------------
		final JPanel panelPlugins = new JPanel(new BorderLayout());
		panelPlugins.add(new JLabel("Installed Plugins"));
		panelPluginsCenter.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		panelWorkspace.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		// filter bar
		tfFilter = new TextFieldFilter("");
		tfFilter.addTextChangeListener(new TextChangeListener() {

			@Override
			public void textChanged(IcyTextField source) {
				String filter = tfFilter.getText();
				panelPluginsCenter.removeAll();
				loadPlugins(filter);
			}
		});

		// plugins in a container separated by letters
		loadPlugins("");

		JScrollPane scrollpane = new JScrollPane(panelPluginsCenter);
		scrollpane.getVerticalScrollBar().setUnitIncrement(16);
		panelPlugins.add(scrollpane, BorderLayout.CENTER);
		// ---------------
		// PANEL WORKSPACE
		// ---------------
		loadWorkspace("sys");

		// PANEL RIGHT
		JPanel panelWorkspaceWithLayout = new JPanel(new BorderLayout());
		JPanel panelOperations = new JPanel();
		panelOperations.setLayout(new BoxLayout(panelOperations, BoxLayout.X_AXIS));
		panelOperations.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		IcyButton btnReload = new IcyButton(new IcyIcon("rot_unclock.png", 24));
		btnReload.setToolTipText("Reload Icy");

		btnReload.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				reloadWorkspace();
				if (ConfirmDialog.confirm("Restart Icy", "Icy is going to restart, do you want to continue?"))
					Icy.exit(true);
			}
		});

		final IcyButton btnNewBand = new IcyButton(new IcyIcon(plusBand, 24));
		btnNewBand.setToolTipText("Click to create a new band for the current workspace");
		btnNewBand.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent actionevent) {
				String workspaceName = currentWorkspace.getName();
				if (workspaceName.equals("sys"))
					MessageDialog.showDialog("System file", "This file should not be modified. It is shown as an example only.");
				else {
					String response = JOptionPane.showInputDialog(Icy.getMainInterface().getMainFrame(),
							"Enter the name of the new band: ", "Band Creation",
							JOptionPane.QUESTION_MESSAGE);
					if (response == null)
						return;
					currentWorkspace.addBand(workspaceName, response);
					currentWorkspace.save();
					reloadWorkspace();
				}
			}
		});

		final IcyButton btnNewWorkspace = new IcyButton(new IcyIcon(plusWorkspace, 24));
		btnNewWorkspace.setToolTipText("Click to create a new workspace here.");
		btnNewWorkspace.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent actionevent) {
				String response = JOptionPane.showInputDialog(Icy.getMainInterface().getMainFrame(), "Enter the name of the Workspace: ",
						"Workspace Creation",
						JOptionPane.QUESTION_MESSAGE);
				if (response == null)
					return;
				Workspace ws = new Workspace();
				ws.setName(response);
				ws.addTask(response);
				ws.save();
				loadWorkspace(response);

				// Create info dialog
				final JDialog dlg = new JDialog(Icy.getMainInterface().getMainFrame());
				dlg.setLayout(new BorderLayout());

				// label used as image
				JLabel lbl = new JLabel(new ImageIcon(imgHelp));
				JPanel panelImg = new JPanel(new GridLayout());
				panelImg.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
				panelImg.add(lbl);

				// south button "OK"
				JButton okbtn = new JButton("OK");
				okbtn.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						dlg.setVisible(false);
					}
				});

				// setup everything
				JPanel panelSouth = new JPanel();
				panelSouth.setLayout(new BoxLayout(panelSouth, BoxLayout.X_AXIS));
				panelSouth.add(Box.createHorizontalGlue());
				panelSouth.add(okbtn);
				panelSouth.add(Box.createHorizontalGlue());
				panelSouth.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));

				dlg.add(panelImg, BorderLayout.CENTER);
				dlg.add(panelSouth, BorderLayout.SOUTH);
				dlg.pack();
				dlg.setLocationRelativeTo(Icy.getMainInterface().getMainFrame());
				dlg.setVisible(true);
			}
		});

		final IcyButton btnRemoveWorkspace = new IcyButton(new IcyIcon(minusWorkspace, 24));
		btnRemoveWorkspace.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent actionevent) {
				if (currentWorkspace == null)
					return;
				if (currentWorkspace.getName().equals("sys")) {
					MessageDialog.showDialog("Deletion error", "This item cannot be deleted");
				} else if (ConfirmDialog.confirm("Deletion",
						"Are you sure you want to delete this workspace? You will not be able to undo this operation.")) {
					FileUtil.delete(currentWorkspace.getLocalFilename(), false);
					loadWorkspace("sys");
				}
			}
		});
		btnProperties.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				PropertiesDialog dlg = PropertiesDialog.getInstance();
				dlg.initGUI(currentWorkspace);
				ComponentUtil.center((Component) dlg);
				dlg.setVisible(true);
				if (dlg.changed() && !currentWorkspace.getName().contentEquals("sys")) {
					String currentName = currentWorkspace.getName();
					if (currentName.contentEquals(dlg.getWkName())) {
						currentWorkspace.setDescription(dlg.getWkDescription());
						currentWorkspace.save();
						boolean enableWorkspace = dlg.isWorkspaceEnabled();
						if (enableWorkspace
								&& (currentWorkspace.getTasks() == null || currentWorkspace.getTasks().get(0).getBands().isEmpty())) {
							MessageDialog.showDialog("Empty Workspace", "Impossible to enable an empty workspace.");
						} else {
							WorkspaceLocalPreferences.setWorkspaceEnable(currentName, enableWorkspace);
						}
					} else {
						HashMap<BandDefinition, ArrayList<ItemDefinition>> hashmap = new HashMap<BandDefinition, ArrayList<ItemDefinition>>();
						String localFilename = currentWorkspace.getLocalFilename();
						TaskDefinition task = currentWorkspace.getTasks().get(0);
						for (BandDefinition currentBand : task.getBands()) {
							hashmap.put(currentBand, currentBand.getItems());
							task.removeBand(currentBand.getName());
						}
						currentWorkspace.addTask(dlg.getWkName());
						TaskDefinition taskNew = currentWorkspace.findTask(dlg.getWkName());
						for (BandDefinition bd : hashmap.keySet()) {
							taskNew.addBand(bd.getName());
							BandDefinition bdNew = taskNew.findBand(bd.getName());
							for (ItemDefinition def : hashmap.get(bd)) {
								bdNew.addItem(def.getClassName(), def.getPriority());
							}
						}
						currentWorkspace.setName(dlg.getWkName());
						currentWorkspace.setDescription(dlg.getWkDescription());
						currentWorkspace.save();
						File f = new File(localFilename);

						// Make sure the file or directory exists and isn't
						// write protected
						if (!f.exists())
							throw new IllegalArgumentException("Delete: no such file or directory: " + localFilename);

						if (!f.canWrite())
							throw new IllegalArgumentException("Delete: write protected: " + localFilename);

						// If it is a directory, make sure it is empty
						if (f.isDirectory()) {
							String[] files = f.list();
							if (files.length > 0)
								throw new IllegalArgumentException("Delete: directory not empty: " + localFilename);
						}

						// Attempt to delete it
						boolean success = f.delete();

						if (!success)
							throw new IllegalArgumentException("Delete: deletion failed");

						reloadWorkspace();
						WorkspaceLocalPreferences.setWorkspaceEnable(currentWorkspace.getName(), dlg.isWorkspaceEnabled());
					}
				}
			}
		});

		// Add the buttons to the south panel
		panelOperations.add(Box.createHorizontalGlue());
		panelOperations.add(btnNewWorkspace);
		panelOperations.add(Box.createHorizontalStrut(5));
		panelOperations.add(btnRemoveWorkspace);
		panelOperations.add(Box.createHorizontalStrut(5));
		panelOperations.add(btnNewBand);
		panelOperations.add(Box.createHorizontalStrut(5));
		panelOperations.add(btnReload);
		panelOperations.add(Box.createHorizontalStrut(5));
		panelOperations.add(btnProperties);
		panelOperations.add(Box.createHorizontalGlue());

		// add the components to the right panel
		panelWorkspaceWithLayout.add(Box.createGlue(), BorderLayout.CENTER);
		panelWorkspaceWithLayout.add(panelWorkspace, BorderLayout.NORTH);

		// Restart info.
		JLabel lblRestartIcy = new JLabel("<html><b>In order to make any change visible, do not forget to reload Icy.</b></html>");
		JPanel southPanel = new JPanel();
		southPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		southPanel.add(lblRestartIcy);

		// set the main panel
		JScrollPane rightScrollPane = new JScrollPane(panelWorkspaceWithLayout, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		rightScrollPane.getVerticalScrollBar().setUnitIncrement(16);

		JPanel panelRight = new JPanel(new BorderLayout());
		panelRight.add(panelOperations, BorderLayout.NORTH);
		panelRight.add(rightScrollPane, BorderLayout.CENTER);

		JPanel panelLeft = new JPanel();
		panelLeft.setLayout(new BorderLayout());
		panelLeft.add(tfFilter, BorderLayout.NORTH);
		panelLeft.add(panelPlugins, BorderLayout.CENTER);

		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelLeft, panelRight);
		splitPane.setDividerSize(6);
		splitPane.setContinuousLayout(true);
		splitPane.setResizeWeight(1);
		mainPanel.setLayout(new BorderLayout());
		mainPanel.add(splitPane, BorderLayout.CENTER);
		mainPanel.add(lblRestartIcy, BorderLayout.SOUTH);
		// mainPanel.add(panelNorth, BorderLayout.NORTH);
		mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "unselect");
		mainPanel.getActionMap().put("unselect", new AbstractAction() {
			/** */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				mainPanel.lastEvent = null;
				_selectedPlugins.clear();
				mainPanel.repaint();
			}
		});

		// set the frame
		frame = new IcyFrame("Workspace editor", true, true, true, true);
		frame.add(new IcyLogo("Workspace Editor"));
		frame.add(mainPanel);
		frame.setVisible(true);
		frame.addToMainDesktopPane();
	}

	/**
	 * Load all the actionable plugins in Icy into the
	 * {@link #panelPluginsCenter};
	 * 
	 * @see {@link PluginLoader#getActionablePlugins()}
	 */
	private void loadPlugins(final String filter) {
		int cpt = 0;
		char actualLetter = '?';

		// ----------------------------
		// Listener for multi selection
		// ----------------------------
		MouseAdapter multiSelectionListener = new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				if (!EventUtil.isControlDown(e))
					_selectedPlugins.clear();
				mainPanel.previouslySelectedPlugins = new ArrayList<PluginDescriptorButton>(_selectedPlugins);
				mainPanel.pointBegin = e.getLocationOnScreen();
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				mainPanel.previouslySelectedPlugins.clear();
				mainPanel.previouslySelectedPlugins = null;
				mainPanel.pointBegin = null;
				mainPanel.pointEnd = null;
				mainPanel.repaint();
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				_selectedPlugins.clear();
				panelPluginsCenter.repaint();
			};

			@Override
			public void mouseDragged(MouseEvent e) {
				mainPanel.pointEnd = e.getLocationOnScreen();
				mainPanel.mouseDragged(e);
			}
		};

		// Create GUI
		// Create an item for every plugin descriptor
		for (PluginDescriptor pd : PluginLoader.getActionablePlugins()) {
			String name = pd.getName();
			if (!name.toLowerCase().contains(filter.toLowerCase()))
				continue;
			char pluginFirstLetter = name.toUpperCase().charAt(0);
			int sizename = name.length();
			if (sizename > minNamePlugin.length())
				minNamePlugin = name;

			// if the first letter is different, the new letter is written
			if (actualLetter != pluginFirstLetter) {
				actualLetter = pluginFirstLetter;
				JLabel lblLetter = new JLabel("   " + actualLetter);
				lblLetter.addMouseListener(multiSelectionListener);
				lblLetter.addMouseMotionListener(multiSelectionListener);
				panelPluginsCenter.add(GuiUtil.besidesPanel(lblLetter));
				++cpt;
			}
			PluginDescriptorButton pdb = new PluginDescriptorButton(pd);
			panelPluginsCenter.add(GuiUtil.besidesPanel(pdb));
			listInstalledPlugins.add(pdb);
			++cpt;
		}
		// Add the refresh button
		IcyButton btnRefresh = new IcyButton("Refresh Plugin List", new IcyIcon("rot_unclock.png"));
		JPanel panelButtonRefresh = new JPanel();
		panelButtonRefresh.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		panelButtonRefresh.setLayout(new BoxLayout(panelButtonRefresh, BoxLayout.X_AXIS));
		panelButtonRefresh.add(Box.createHorizontalGlue());
		panelButtonRefresh.add(btnRefresh);
		panelButtonRefresh.add(Box.createHorizontalGlue());
		btnRefresh.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				_selectedPlugins.clear();
				panelPluginsCenter.removeAll();
				loadPlugins("");
			}
		});
		// add space in case there is a too big space below
		for (;cpt <= 10; ++cpt) {
			panelButtonRefresh.add(new JLabel(""));
		}
		panelPluginsCenter.setLayout(new GridLayout(cpt + 2, 1));
		panelPluginsCenter.add(panelButtonRefresh);
		panelPluginsCenter.revalidate();
		panelPluginsCenter.repaint();
	}

	/**
	 * Reload the current workspace.
	 */
	void reloadWorkspace() {
		if (currentWorkspace != null)
			loadWorkspace(currentWorkspace.getName());
	}

	/**
	 * Load the workspace <code>workspacename</code>
	 * 
	 * @param workspacename
	 *            : name of the {@link Workspace}. Can contain the .xml
	 *            extension.
	 */
	private void loadWorkspace(String workspacename) {
		// -------------------
		// Create the ComboBox
		// -------------------
		File workspacesDir = new File("workspace");
		if (!workspacesDir.exists() || !workspacesDir.isDirectory()) {
			return;
		}
		String[] files = workspacesDir.list(new FilenameFilter() {
			@Override
			public boolean accept(File file, String s) {
				return s.endsWith(".xml");
			}
		});

		// Create the list used as model for the combo box
		String[] cbModel = new String[files.length];
		System.arraycopy(files, 0, cbModel, 0, files.length);
		for (int i = 0; i < cbModel.length; ++i) {
			if (cbModel[i].endsWith(".xml"))
				cbModel[i] = cbModel[i].substring(0, cbModel[i].length() - 4);
		}

		// creation of the combo box + listener
		final JComboBox cbChooseWorkspace = new JComboBox(cbModel);
		if (workspacename != null)
			cbChooseWorkspace.setSelectedItem(workspacename);
		cbChooseWorkspace.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent itemevent) {
				loadWorkspace((String) cbChooseWorkspace.getSelectedItem());
				mainPanel.setSize(mainPanel.getSize());
			}
		});

		// ------------------
		// DRAW THE COMPONENT
		// ------------------
		panelWorkspace.removeAll();
		panelWorkspace.setLayout(new BoxLayout(panelWorkspace, BoxLayout.Y_AXIS));
		panelWorkspace.setSize(new Dimension(150, 60));
		JPanel panelChoose = new JPanel();
		panelChoose.setLayout(new BoxLayout(panelChoose, BoxLayout.X_AXIS));
		panelChoose.add(new JLabel("<html><b>Choose workspace</b></html>"));
		panelChoose.add(Box.createHorizontalGlue());
		panelWorkspace.add(panelChoose);
		panelWorkspace.add(cbChooseWorkspace);
		panelWorkspace.add(Box.createVerticalStrut(6));

		if (workspacename != null)
			currentWorkspace = new Workspace(workspacename);
		else
			currentWorkspace = null;
		if (currentWorkspace != null && !currentWorkspace.getTasks().isEmpty()) {
			final TaskDefinition task = currentWorkspace.getTasks().get(0);
			// ------------
			// POPUP PANELS
			// ------------
			int bandIndex = 0;
			final LinkedList<BandDefinition> bandsCopy = new LinkedList<BandDefinition>(task.getBands());
			for (final BandDefinition band : bandsCopy) {
				final int finalBandIndex = bandIndex;
				final PopupPanel ppp = new PopupPanel(band.getName());
				final JPanel pppMainPanel = ppp.getMainPanel();
				pppMainPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

				// listener for deletion
				MouseListener listenerBandDelete = new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						if (e.getButton() == MouseEvent.BUTTON3) {
							TitledPopupMenu popup = new TitledPopupMenu("Band: " + bandsCopy.get(finalBandIndex).getName());
							Border titleUnderline = BorderFactory.createMatteBorder(1, 0, 0, 0, popup.getForeground());
							TitledBorder labelBorder = BorderFactory.createTitledBorder(titleUnderline, popup.getLabel(),
									TitledBorder.CENTER, TitledBorder.ABOVE_TOP, ppp
											.getFont().deriveFont(Font.BOLD), popup.getForeground());
							popup.setBorder(BorderFactory.createCompoundBorder(popup.getBorder(), labelBorder));
							popup.setLocation(e.getLocationOnScreen());

							JMenuItem itemRenameBand = new JMenuItem("Rename Band");
							itemRenameBand.addActionListener(new ActionListener() {
								// TODO
								@Override
								public void actionPerformed(ActionEvent e) {
									if (currentWorkspace.getName().contentEquals("sys")) {
										MessageDialog
												.showDialog("Cannot be modified",
														"The bands of this workspace cannot be moved. The only possible action is the removal of plugins from band \"new\".");
										return;
									}
									String response = (String) JOptionPane.showInputDialog(Icy.getMainInterface().getMainFrame(),
											"Enter the name of the new band: ",
											"Band Creation", JOptionPane.QUESTION_MESSAGE, null, null, band.getName());
									if (response == null)
										return;
									HashMap<String, ArrayList<ItemDefinition>> hashmap = new HashMap<String, ArrayList<ItemDefinition>>();

									// get a copy of every band in the task
									// and delete it from the task
									for (BandDefinition currentBand : bandsCopy) {
										if (currentBand == band)
											hashmap.put(response, currentBand.getItems());
										else
											hashmap.put(currentBand.getName(), currentBand.getItems());
										task.removeBand(currentBand.getName());
									}

									// Reconstruct in the right order
									for (String bandName : hashmap.keySet()) {
										task.addBand(bandName);
										for (ItemDefinition item : hashmap.get(bandName)) {
											task.addItem(bandName, item.getClassName());
											task.findItem(item.getClassName()).setPriority(item.getPriority());
										}
									}
									currentWorkspace.save();
									reloadWorkspace();
								}
							});
							popup.add(itemRenameBand);

							// Move the band up
							JMenuItem itemMoveUp = new JMenuItem("Move Up");
							itemMoveUp.addActionListener(new ActionListener() {

								@Override
								public void actionPerformed(ActionEvent e) {
									if (currentWorkspace.getName().contentEquals("sys")) {
										MessageDialog
												.showDialog("Cannot be modified",
														"The bands of this workspace cannot be moved. The only possible action is the removal of plugins from band \"new\".");
										return;
									}
									HashMap<BandDefinition, ArrayList<ItemDefinition>> hashmap = new HashMap<BandDefinition, ArrayList<ItemDefinition>>();

									bandsCopy.remove(band);
									bandsCopy.add(finalBandIndex - 1, band);

									// get a copy of every band in the task
									// and delete it from the task
									for (BandDefinition currentBand : bandsCopy) {
										hashmap.put(currentBand, currentBand.getItems());
										task.removeBand(currentBand.getName());
									}

									// Reconstruct in the right order
									for (BandDefinition currentBand : bandsCopy) {
										String bandName = currentBand.getName();
										task.addBand(bandName);
										for (ItemDefinition item : hashmap.get(currentBand)) {
											task.addItem(bandName, item.getClassName());
											task.findItem(item.getClassName()).setPriority(item.getPriority());
										}
									}
									currentWorkspace.save();
									reloadWorkspace();
								}
							});
							popup.add(itemMoveUp);

							// Move the band down
							JMenuItem itemMoveDown = new JMenuItem("Move Down");
							itemMoveDown.addActionListener(new ActionListener() {

								@Override
								public void actionPerformed(ActionEvent e) {
									if (currentWorkspace.getName().contentEquals("sys")) {
										MessageDialog
												.showDialog("Cannot be modified",
														"The bands of this workspace cannot be moved. The only possible action is the removal of plugins from band \"new\".");
										return;
									}
									HashMap<BandDefinition, ArrayList<ItemDefinition>> hashmap = new HashMap<BandDefinition, ArrayList<ItemDefinition>>();

									bandsCopy.remove(band);
									bandsCopy.add(finalBandIndex + 1, band);

									// get a copy of every band in the task
									// and delete it from the task
									for (BandDefinition currentBand : bandsCopy) {
										hashmap.put(currentBand, currentBand.getItems());
										task.removeBand(currentBand.getName());
									}

									// Reconstruct in the right order
									for (BandDefinition currentBand : bandsCopy) {
										String bandName = currentBand.getName();
										task.addBand(bandName);
										for (ItemDefinition item : hashmap.get(currentBand)) {
											task.addItem(bandName, item.getClassName());
											task.findItem(item.getClassName()).setPriority(item.getPriority());
										}
									}
									currentWorkspace.save();
									reloadWorkspace();
								}
							});
							popup.add(itemMoveDown);

							// Remove Band Item
							JMenuItem itemDeleteBand = new JMenuItem("Remove Band");
							itemDeleteBand.addActionListener(new ActionListener() {

								@Override
								public void actionPerformed(ActionEvent e) {
									if (currentWorkspace.getName().contentEquals("sys")) {
										MessageDialog
												.showDialog("Cannot be deleted",
														"The bands of this workspace cannot be deleted. The only possible action is the removal of plugins from band \"new\".");
										return;
									} else if (ConfirmDialog.confirm("Band deletion", "Are you sure you want to delete this band?")) {
										task.removeBand(band);
										currentWorkspace.save();
										reloadWorkspace();
										if (currentWorkspace.getTasks() == null
												|| currentWorkspace.getTasks().get(0).getBands().size() <= 1) {
											WorkspaceLocalPreferences.setWorkspaceEnable(currentWorkspace.getName(), false);
										}
									}
								}
							});
							popup.add(itemDeleteBand);

							// Display
							if (finalBandIndex < 1) {
								itemMoveUp.setEnabled(false);
							} else if (finalBandIndex >= bandsCopy.size() - 1) {
								itemMoveDown.setEnabled(false);
							}
							popup.show(e.getComponent(), e.getX(), e.getY());
						}
					}
				};

				// listener for mouse over
				// (used by mainpanel to know where to drop items)
				MouseListener listenerMouseOver = new MouseAdapter() {

					@Override
					public void mouseEntered(MouseEvent mouseevent) {
						mainPanel.currentBand = band;
					}

					@Override
					public void mouseExited(MouseEvent mouseevent) {
						mainPanel.currentBand = null;
					}
				};
				ppp.getComponent(0).addMouseListener(listenerBandDelete);
				pppMainPanel.addMouseListener(listenerBandDelete);
				ppp.getComponent(0).addMouseListener(listenerMouseOver);
				pppMainPanel.addMouseListener(listenerMouseOver);

				pppMainPanel.setLayout(new BoxLayout(pppMainPanel, BoxLayout.Y_AXIS));
				if (band.getItems().size() == 0) {
					pppMainPanel.add(Box.createRigidArea(new Dimension(20, 20)));
				} else {
					// -------
					// Create every item based on itemdefinition
					// ------
					for (final ItemDefinition itd : band.getItems()) {
						JPanel panelPlugin = new JPanel();
						panelPlugin.setLayout(new BoxLayout(panelPlugin, BoxLayout.X_AXIS));
						final String classname = itd.getClassName();
						panelPlugin.setMinimumSize(new Dimension(60, 20));
						JLabel pluginName = new JLabel(classname.substring(classname.lastIndexOf('.') + 1, classname.length()));
						pluginName.addMouseListener(new PluginActionAdapter(task, band, ppp, classname));
						pluginName.setMinimumSize(new Dimension(100, 20));
						panelPlugin.add(pluginName);
						panelPlugin.add(Box.createHorizontalGlue());
						switch (itd.getPriority()) {
						case LOW:
							panelPlugin.add(new SizeButton(itd, 0));
							break;
						case MEDIUM:
							panelPlugin.add(new SizeButton(itd, 1));
							break;
						case TOP:
							panelPlugin.add(new SizeButton(itd, 2));
							break;
						}
						pppMainPanel.add(panelPlugin);
					}
					pppMainPanel.validate();
				}
				panelWorkspace.add(ppp);
				panelWorkspace.add(Box.createVerticalStrut(5));
				ppp.expand();
				++bandIndex;
			}

		} else {
			panelWorkspace.add(new JLabel("Empty Workspace"));
		}
		btnProperties.setEnabled(!currentWorkspace.getName().contentEquals("sys"));
		if (frame != null)
			frame.revalidate();
	}

	/**
	 * Class used for the Drag'n Drop functionalities.
	 * 
	 * @author thomasprovoost
	 */
	private class MainPanel extends JPanel implements MouseListener, MouseMotionListener {

		protected Point pointEnd;
		protected Point pointBegin;
		protected BandDefinition currentBand;
		protected Rectangle selectionRectangle = new Rectangle(0, 0, 0, 0);
		protected ArrayList<PluginDescriptorButton> previouslySelectedPlugins;
		protected MouseEvent lastEvent;

		public MainPanel() {
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = 1462696193771472039L;

		@Override
		public void mouseDragged(MouseEvent e) {
			if (previouslySelectedPlugins != null) {
				_selectedPlugins.clear(); // clear the list of selected plugins
				// the previouslySelectedPlugins
				// list already contains all needed
				// information
				// this step is only for display
				// purpose

				// rectangle of selection
				Point location = getLocationOnScreen();
				Point pStart = new Point(pointBegin.x < pointEnd.x ? pointBegin.x : pointEnd.x, pointBegin.y < pointEnd.y ? pointBegin.y
						: pointEnd.y);
				int w = (pStart.x == pointBegin.x ? pointEnd.x - pointBegin.x : pointBegin.x - pointEnd.x);
				int h = (pStart.y == pointBegin.y ? pointEnd.y - pointBegin.y : pointBegin.y - pointEnd.y);
				selectionRectangle.setBounds(pStart.x - location.x, pStart.y - location.y, w, h);

				// add selected plugins in the list
				for (PluginDescriptorButton pdb : listInstalledPlugins) {
					Rectangle bounds = pdb.getBounds();
					bounds.x = pdb.getLocationOnScreen().x - location.x;
					bounds.y = pdb.getLocationOnScreen().y - location.y;
					if (selectionRectangle.intersects(bounds)) {
						pdb.setSelected(true);
					}
				}
				for (PluginDescriptorButton pdb : previouslySelectedPlugins) {
					pdb.setSelected(true);
				}
			} else if (!_selectedPlugins.isEmpty()) {
				lastEvent = e;
			}
			repaint();
		}

		@Override
		public void mouseMoved(MouseEvent e) {
		}

		@Override
		public void paint(Graphics g) {
			super.paint(g);
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			if (lastEvent != null) {
				// DRAW THE SELECTED PLUGINS IN
				// TRANSPARENT
				Point actual = getLocationOnScreen();
				for (PluginDescriptorButton pdb : new ArrayList<PluginDescriptorButton>(_selectedPlugins)) {
					Point objectLocation = pdb.getLocationOnScreen();
					int w = pdb.imgRenderer.getWidth();
					int h = pdb.imgRenderer.getHeight();
					int[] data = new int[w * h];
					pdb.imgRenderer.getRGB(0, 0, w, h, data, 0, w);
					for (int i = 0; i < data.length; ++i) {
						data[i] &= 0xFFFFFF;
						data[i] |= 80 << 24;
					}
					BufferedImage img2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
					img2.setRGB(0, 0, w, h, data, 0, w);
					g2.drawImage(img2, objectLocation.x - actual.x + lastEvent.getX(), objectLocation.y - actual.y + lastEvent.getY(), null);
				}
			}
			if (pointBegin != null && pointEnd != null) {
				g2.setColor(Color.GRAY);
				g2.draw(selectionRectangle);
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if (lastEvent != null) {
				Point pW = panelWorkspace.getLocationOnScreen();
				Point pE = e.getLocationOnScreen();
				// tests if mouse release is in the panelWorkspace
				if (pE.x >= pW.x && pE.x <= pW.x + panelWorkspace.getWidth() && pE.y >= pW.y && pE.y <= pE.y + panelWorkspace.getHeight()) {
					// if null, ask for a band creation
					if (currentBand == null) {
						if (currentWorkspace.getName().equals("sys") || currentWorkspace.getName().equals("sys.xml"))
							MessageDialog.showDialog("System file", "This file should not be modified. It is shown as an example only.");
						else {
							String response = JOptionPane.showInputDialog(Icy.getMainInterface().getMainFrame(),
									"Enter the name of the new band: ", "Band Creation",
									JOptionPane.QUESTION_MESSAGE);
							if (response != null) {
								currentWorkspace.addBand(currentWorkspace.getName(), response);
								currentBand = currentWorkspace.findBand(currentWorkspace.getName(), response);
							}
						}
					}
					// if still null, do nothing
					if (currentBand != null) {
						for (PluginDescriptorButton pdb : new ArrayList<PluginDescriptorButton>(_selectedPlugins)) {
							String className = pdb.pd.getClassName();
							if (currentBand != null && currentBand.findItem(className) == null) {
								currentBand.addItem(className);
								for (ItemDefinition itd : currentBand.getItems()) {
									if (itd.getClassName() == className)
										itd.setPriority(RibbonElementPriority.MEDIUM);
								}
							}
						}
						currentWorkspace.save();
						reloadWorkspace();
					}
					_selectedPlugins.clear();
				}
				mainPanel.repaint();
				currentBand = null;
			}
		}

		@Override
		public void mouseClicked(MouseEvent mouseevent) {
		}

		@Override
		public void mouseEntered(MouseEvent mouseevent) {
		}

		@Override
		public void mouseExited(MouseEvent mouseevent) {
		}

		@Override
		public void mousePressed(MouseEvent mouseevent) {
		}
	}

	/**
	 * JLabel modified to contain the {@link PluginDescriptor}.
	 * 
	 * @author thomasprovoost
	 */
	private class PluginDescriptorButton extends JLabel {

		PluginDescriptor pd;
		boolean preferredSizeSet = false;
		boolean mouseOver = false;
		BufferedImage imgRenderer;

		/**
		 * Default serial version UID
		 */
		private static final long serialVersionUID = 1L;

		public PluginDescriptorButton(PluginDescriptor pd) {
			this.pd = pd;
			setText(pd.getName());
			setToolTipText(pd.getClassName());
			MouseAdapter mouseadapter = new PDBMouseAdapter(this);
			addMouseMotionListener(mouseadapter);
			addMouseListener(mouseadapter);
		}

		@Override
		public void paint(Graphics g) {
			// test if preferred size already set
			if (!preferredSizeSet) {
				setPreferredSize(new Dimension(g.getFontMetrics().charsWidth(minNamePlugin.toCharArray(), 0, minNamePlugin.length()) + 26,
						40));
				preferredSizeSet = true;
			}

			// initialize variables
			int w = getWidth();
			int h = getHeight();
			String text = getText();
			imgRenderer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			Graphics2D gb = (Graphics2D) imgRenderer.getGraphics();
			gb.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			gb.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			gb.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

			// drawing
			gb.setFont(new Font("Arial", Font.BOLD, 12));
			SubstanceColorScheme cs;
			if (isSelected()) {
				cs = SubstanceColorSchemeUtilities.getColorScheme(new JButton(), ComponentState.ROLLOVER_SELECTED);
			} else if (mouseOver) {
				cs = SubstanceColorSchemeUtilities.getColorScheme(new JButton(), ComponentState.SELECTED);
			} else {
				cs = SubstanceColorSchemeUtilities.getColorScheme(new JButton(), ComponentState.ENABLED);
			}
			Color background = cs.getSelectionBackgroundColor();
			Color foreground = cs.getForegroundColor();
			Paint defaultPaint = gb.getPaint();
			if (isSelected()) {
				gb.setPaint(new GradientPaint(w / 2, 0, background, w / 2, h / 2, background.darker(), true));
			} else {
				gb.setPaint(new GradientPaint(w / 2, 0, background, w / 2, h / 2, background.brighter(), true));
			}
			gb.fillRoundRect(1, 1, w - 1, h - 1, 10, 10);
			gb.setPaint(defaultPaint);
			gb.setColor(foreground);
			gb.drawRoundRect(1, 1, w - 2, h - 2, 10, 10);
			gb.drawString(text, 10 + w / 2 - gb.getFontMetrics().charsWidth(text.toCharArray(), 0, text.length()) / 2, h / 2
					+ gb.getFontMetrics().getHeight() / 2);
			gb.drawImage(pd.getIconAsImage(), 5, 5, h - 10, h - 10, null);
			g.drawImage(imgRenderer, 0, 0, w, h, null);
			gb.dispose();
		}

		/**
		 * Toggle the {@link #isSelected()} value.
		 */
		void toggleSelected() {
			if (isSelected())
				setSelected(false);
			else
				setSelected(true);
		}

		/**
		 * Return if the current plugin is selected.
		 * 
		 * @see #toggleSelected(), {@link #setSelected(boolean)}
		 */
		public boolean isSelected() {
			for (PluginDescriptorButton pdb : _selectedPlugins) {
				if (pdb.equals(this)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Set the selected flag to <code>selected</code> value.
		 * 
		 * @param selected
		 */
		public void setSelected(boolean selected) {
			if (selected)
				_selectedPlugins.add(this);
			else if (!selected)
				_selectedPlugins.remove(this);
			repaint();
		}

		@Override
		public String toString() {
			return getText();
		}
	}

	/**
	 * Specific button created to change the size of the plugin in the
	 * workspace.
	 * 
	 * @author thomasprovoost
	 */
	private class SizeButton extends JButton implements ActionListener {

		/** */
		private static final long serialVersionUID = 1L;
		int type = 0;
		ItemDefinition itd;

		public SizeButton(ItemDefinition itd, int type) {
			this.type = type;
			this.itd = itd;
			setSize(32, 32);
			// setPreferredSize(new Dimension(32, 32));
			changeIcon();
			addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			type = ++type % 3;
			switch (type) {
			case 0:
				itd.setPriority(RibbonElementPriority.LOW);
				break;
			case 1:
				itd.setPriority(RibbonElementPriority.MEDIUM);
				break;
			case 2:
				itd.setPriority(RibbonElementPriority.TOP);
				break;
			}
			changeIcon();
			currentWorkspace.save();
		}

		private void changeIcon() {
			switch (type) {
			case 0:
				setIcon(smallButton);
				setToolTipText("Only a small icon");
				break;
			case 1:
				setIcon(mediumButton);
				setToolTipText("Small icon and name of the plugin");
				break;
			case 2:
				setIcon(largeButton);
				setToolTipText("Large icon and name of the plugin");
				break;
			default:
				System.out.println("error : " + type);
			}
		}
	}

	class PDBMouseAdapter extends MouseAdapter {

		PluginDescriptorButton pdb;

		public PDBMouseAdapter(PluginDescriptorButton pdb) {
			this.pdb = pdb;
		}

		@Override
		public void mousePressed(MouseEvent e) {
			pdb.setSelected(true);
			if (EventUtil.isControlDown(e)) {
				// starts multi selection
				mainPanel.previouslySelectedPlugins = new ArrayList<PluginDescriptorButton>(_selectedPlugins);
				mainPanel.pointBegin = e.getLocationOnScreen();
			}
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if (mainPanel.previouslySelectedPlugins != null)
				mainPanel.pointEnd = e.getLocationOnScreen();
			else
				mainPanel.lastEvent = e;
			mainPanel.mouseDragged(e);
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			pdb.repaint();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// equivalent of MouseClicked
			if (e.getClickCount() > 0) {
				pdb.toggleSelected();
				if (!_selectedPlugins.isEmpty()) {
					if (e.isShiftDown()) {
						if (mainPanel.previouslySelectedPlugins == null) {
							mainPanel.previouslySelectedPlugins = new ArrayList<PluginDescriptorButton>();
							mainPanel.previouslySelectedPlugins.add(_selectedPlugins.get(_selectedPlugins.size() - 1));
						}
						if (!EventUtil.isControlDown(e)) {
							for (PluginDescriptorButton pdb : new ArrayList<PluginDescriptorButton>(_selectedPlugins)) {
								pdb.setSelected(false);
							}
							_selectedPlugins.clear();
						}
						int idxCurrent = listInstalledPlugins.indexOf(pdb);
						int idxLastSelected = listInstalledPlugins.indexOf(mainPanel.previouslySelectedPlugins.get(0));
						if (idxCurrent != idxLastSelected) {
							mainPanel.previouslySelectedPlugins.get(0).setSelected(true);
							if (idxCurrent > idxLastSelected) {
								idxLastSelected = idxCurrent + idxLastSelected;
								idxCurrent = idxLastSelected - idxCurrent;
								idxLastSelected = idxLastSelected - idxCurrent;
							}
							for (int i = idxCurrent; i != idxLastSelected; i++) {
								PluginDescriptorButton currentPdb = listInstalledPlugins.get(i);
								if (currentPdb != this.pdb)
									currentPdb.setSelected(true);
							}
						}

					} else {
						mainPanel.previouslySelectedPlugins = null;
						if (!EventUtil.isControlDown(e)) {
							for (PluginDescriptorButton pdb : new ArrayList<PluginDescriptorButton>(_selectedPlugins)) {
								pdb.setSelected(false);
							}
							_selectedPlugins.clear();
						}
					}
				}
				pdb.toggleSelected();
			}

			// normal MousePressed
			if (mainPanel.previouslySelectedPlugins != null) {
				mainPanel.previouslySelectedPlugins.clear();
				mainPanel.previouslySelectedPlugins = null;
				mainPanel.pointBegin = null;
				mainPanel.pointEnd = null;
				mainPanel.repaint();
			} else {
				mainPanel.mouseReleased(e);
				mainPanel.lastEvent = null;
			}
			pdb.repaint();
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			pdb.mouseOver = true;
			pdb.repaint();
		}

		@Override
		public void mouseExited(MouseEvent e) {
			pdb.repaint();
			pdb.mouseOver = false;
		}
	}

	class PluginActionAdapter extends MouseAdapter {

		private TaskDefinition task;
		private String className;
		private BandDefinition band;
		private PopupPanel ppp;

		public PluginActionAdapter(TaskDefinition task, BandDefinition band, PopupPanel ppp, String className) {
			this.task = task;
			this.band = band;
			this.ppp = ppp;
			this.className = className;
		}

		@Override
		public void mouseClicked(final MouseEvent e) {

			// Display a popup panel when user presses Mouse
			// Button 3
			if (e.getButton() == MouseEvent.BUTTON3) {

				final ItemDefinition clickedItem = task.findItem(className);
				final ArrayList<ItemDefinition> items = band.getItems(); // copy
																			// of
																			// items
				final int clickedItemIndex = items.indexOf(clickedItem);
				TitledPopupMenu popup;
				Border titleUnderline;
				TitledBorder labelBorder;

				// Create popup menu
				popup = new TitledPopupMenu("Plugin: " + className.substring(className.lastIndexOf(".") + 1));
				titleUnderline = BorderFactory.createMatteBorder(1, 0, 0, 0, popup.getForeground());
				labelBorder = BorderFactory.createTitledBorder(titleUnderline, popup.getLabel(), TitledBorder.CENTER,
						TitledBorder.ABOVE_TOP, ppp.getFont().deriveFont(Font.BOLD),
						popup.getForeground());
				popup.setBorder(BorderFactory.createCompoundBorder(popup.getBorder(), labelBorder));
				popup.setLocation(e.getLocationOnScreen());

				// Create items in popup
				JMenuItem itemMoveUp = new JMenuItem("Move Up");
				itemMoveUp.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						band.clear();
						items.remove(clickedItemIndex);
						items.add(clickedItemIndex - 1, clickedItem);
						for (ItemDefinition currentItem : items) {
							String currentClassName = currentItem.getClassName();
							task.addItem(band.getName(), currentClassName);
							task.findItem(currentClassName).setPriority(currentItem.getPriority());
						}
						currentWorkspace.save();
						reloadWorkspace();
					}
				});
				itemMoveUp.setEnabled(clickedItemIndex > 0);
				popup.add(itemMoveUp);

				JMenuItem itemMoveDown = new JMenuItem("Move Down");
				itemMoveDown.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						band.clear();
						items.remove(clickedItemIndex);
						items.add(clickedItemIndex + 1, clickedItem);
						for (ItemDefinition currentItem : items) {
							String currentClassName = currentItem.getClassName();
							task.addItem(band.getName(), currentClassName);
							task.findItem(currentClassName).setPriority(currentItem.getPriority());
						}
						currentWorkspace.save();
						reloadWorkspace();
					}
				});
				itemMoveDown.setEnabled(clickedItemIndex < items.size() - 1);
				popup.add(itemMoveDown);

				// item delete plugin
				JMenuItem itemDeletePlugin = new JMenuItem("Remove Plugin from band");
				itemDeletePlugin.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e2) {
						if (currentWorkspace.getName().contentEquals("sys") && !band.getName().contentEquals("New")) {
							MessageDialog.showDialog("Cannot be deleted", "Only possible with band \"new\".");
							return;
						}
						if (ConfirmDialog.confirm("Plugin removal", "Are you sure you want to remove this plugin from this band?")) {
							band.removeItem(className);
							currentWorkspace.save();
							reloadWorkspace();
						}
					}
				});
				popup.add(itemDeletePlugin);

				// item delete band
				JMenuItem itemDeleteBand = new JMenuItem("Remove Band");
				itemDeleteBand.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						if (currentWorkspace.getName().contentEquals("sys")) {
							MessageDialog
									.showDialog("Cannot be deleted",
											"The bands of this workspace cannot be deleted. The only possible action is the removal of plugins from band \"new\".");
							return;
						}
						if (ConfirmDialog.confirm("Band deletion", "Are you sure you want to delete this band?")) {
							task.removeBand(band);
							currentWorkspace.save();
							reloadWorkspace();
						}
					}
				});
				popup.add(itemDeleteBand);
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}
}
