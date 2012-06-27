package plugins.tprovoost.workspaceeditor;

import icy.main.Icy;
import icy.preferences.WorkspaceLocalPreferences;
import icy.workspace.Workspace;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

public class PropertiesDialog extends JDialog {

	private static PropertiesDialog singleton = new PropertiesDialog();

	/** Default serial UID */
	private static final long serialVersionUID = 1L;

	/** {@link JTextField} containing the name of the workspace. */
	private JTextField tfWorkspaceName;

	/** {@link JTextArea} containing the description of the workspace. */
	private JTextField tfWorkspaceDesc;

	private JCheckBox cboxEnabled;

	/** result of the dialog */
	private boolean changed = false;

	private PropertiesDialog() {
		super(Icy.getMainInterface().getMainFrame(), "Properties", ModalityType.APPLICATION_MODAL);

		getContentPane().setLayout(new BorderLayout());

		JPanel panel = new JPanel();
		JRootPane root = getRootPane();
		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "quit");
		root.getActionMap().put("quit", new AbstractAction() {
			/** */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		panel.setBorder(new EmptyBorder(4, 4, 4, 4));
		getContentPane().add(panel);
		panel.setLayout(new GridLayout(6, 1));

		JLabel lblWorkspaceName = new JLabel("<html><b>Name:</b></html>");
		panel.add(lblWorkspaceName);

		tfWorkspaceName = new JTextField("");
		panel.add(tfWorkspaceName);
		tfWorkspaceName.setColumns(10);

		JLabel lblWorkspaceDescription = new JLabel("<html><b>Description:</b></html>");
		panel.add(lblWorkspaceDescription);

		tfWorkspaceDesc = new JTextField("");
		panel.add(tfWorkspaceDesc);

		JPanel panelEnabled = new JPanel();
		panelEnabled.setLayout(new BoxLayout(panelEnabled, BoxLayout.X_AXIS));
		cboxEnabled = new JCheckBox("Enabled");
		panelEnabled.add(Box.createHorizontalGlue());
		panelEnabled.add(cboxEnabled);
		panelEnabled.add(Box.createHorizontalGlue());
		panel.add(panelEnabled);

		JPanel panelButtons = new JPanel();
		panelButtons.setBorder(new EmptyBorder(4, 10, 4, 10));
		panel.add(panelButtons);

		JButton btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		panelButtons.setLayout(new BoxLayout(panelButtons, BoxLayout.X_AXIS));
		panelButtons.add(btnCancel);

		JButton btnOk = new JButton("OK");
		btnOk.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				changed = true;
				setVisible(false);
			}
		});

		Component horizontalGlue = Box.createHorizontalGlue();
		panelButtons.add(horizontalGlue);
		panelButtons.add(Box.createHorizontalStrut(10));
		Component horizontalGlue2 = Box.createHorizontalGlue();
		panelButtons.add(horizontalGlue2);
		panelButtons.add(btnOk);
		
		pack();
	}

	public boolean changed() {
		return changed;
	}

	public String getWkName() {
		return tfWorkspaceName.getText();
	}

	public String getWkDescription() {
		return tfWorkspaceDesc.getText();
	}

	public boolean isWorkspaceEnabled() {
		return cboxEnabled.isSelected();
	}

	public static PropertiesDialog getInstance() {
		return singleton;
	}

	public void initGUI(Workspace currentWorkspace) {
		tfWorkspaceName.setText(currentWorkspace.getName());
		tfWorkspaceDesc.setText(currentWorkspace.getDescription());
		cboxEnabled.setSelected(WorkspaceLocalPreferences.isWorkspaceEnable(currentWorkspace.getName()));
		repaint();
	}
}
