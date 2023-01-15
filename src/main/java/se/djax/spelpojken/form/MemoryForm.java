package se.djax.spelpojken.form;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;

import se.djax.spelpojken.Cpu;
import se.djax.spelpojken.GameBoy;
import se.djax.spelpojken.model.CpuModel;
import se.djax.spelpojken.model.MemoryModel;

public class MemoryForm extends JFrame {
	private static final long serialVersionUID = -3005200112393123586L;

	private final Cpu cpu;
	private final GameBoy gameBoy;
	private JLabel lblAddressInfo;
	private MemoryModel memoryModel;

	public MemoryForm(GameBoy gameBoy) {
		this.gameBoy = gameBoy;
		this.cpu = gameBoy.getCpu();
		setTitle("Memory");
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		memoryModel = new MemoryModel(gameBoy);
		gameBoy.addListener(memoryModel);
		JTable table = new JTable(memoryModel);
		table.getColumnModel().getColumn(0).setMinWidth(70);
		table.setDefaultRenderer(Object.class, memoryModel.new MemoryModelRenderer());

		getContentPane().add(
				new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS));

		// Cpu
		CpuModel cpuModel = new CpuModel(cpu);
		gameBoy.addListener(cpuModel);
		JTable cpuTable = new JTable(cpuModel);

		getContentPane().add(new JScrollPane(cpuTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS), BorderLayout.EAST);

		// Toolbar
		JToolBar bottomToolbar = new JToolBar();

		JButton btnStep = new JButton("Step");
		btnStep.addActionListener(e -> onStepClicked());
		bottomToolbar.add(btnStep);

		JButton btnRun = new JButton("Run");
		btnRun.addActionListener(e -> onRunClicked());
		bottomToolbar.add(btnRun);

		lblAddressInfo = new JLabel("");
		bottomToolbar.add(lblAddressInfo);

		add(bottomToolbar, BorderLayout.SOUTH);

		// Klick
		table.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent me) {
				JTable table = (JTable) me.getSource();
				Point p = me.getPoint();
				int row = table.rowAtPoint(p);
				int col = table.columnAtPoint(p);
				if (me.getClickCount() == 1 && row != -1) {
					onClick(row, col);
				}
			}
		});

		setSize(1300, 600);

	}

	protected void onClick(int row, int col) {
		lblAddressInfo.setText(memoryModel.getInfo(row, col));
	}

	private void onRunClicked() {
		new Thread(() -> {
			while (true) {
				try {
					gameBoy.step();
					// Thread.sleep(0, 1);
					if (gameBoy.getCpu().pc == 0xC093) {
						// return;
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}).start();
	}

	protected void onStepClicked() {
		gameBoy.step();
	}

}
