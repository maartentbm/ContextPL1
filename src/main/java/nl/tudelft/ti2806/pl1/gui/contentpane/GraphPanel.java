package nl.tudelft.ti2806.pl1.gui.contentpane;

import java.awt.Dimension;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

import nl.tudelft.ti2806.pl1.DGraph.ConvertDGraph;
import nl.tudelft.ti2806.pl1.DGraph.DGraph;
import nl.tudelft.ti2806.pl1.gui.Event;
import nl.tudelft.ti2806.pl1.gui.ProgressDialog;
import nl.tudelft.ti2806.pl1.gui.Window;
import nl.tudelft.ti2806.pl1.gui.optionpane.GenomeRow;
import nl.tudelft.ti2806.pl1.gui.optionpane.GenomeTableObserver;
import nl.tudelft.ti2806.pl1.reader.NodePlacer;
import nl.tudelft.ti2806.pl1.reader.Reader;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerListener;
import org.graphstream.ui.view.ViewerPipe;

/**
 * @author Maarten
 * 
 */
public class GraphPanel extends JSplitPane implements NodeSelectionObserver {

	/** The serial version UID. */
	private static final long serialVersionUID = -3581428828970208653L;

	/**
	 * The default location for the divider between the graph view and the text
	 * area.
	 */
	private static final int DEFAULT_DIVIDER_LOC = 300;

	/**
	 * TODO the temp hard coded size of the view.
	 */
	private static final Dimension VIEW_SIZE = new Dimension(100000, 500);

	/**
	 * The list of node selection observers.
	 * 
	 * @see NodeSelectionObserver
	 */
	private List<NodeSelectionObserver> observers = new ArrayList<NodeSelectionObserver>();

	/** The window this panel is part of. */
	private Window window;

	/** The top pane showing the graph. */
	private JScrollPane graphPane;

	/** The bottom pane showing node content. */
	private JScrollPane infoPane;

	/** The graph loaded into the panel. */
	private Graph graph;

	/** The last selected node. */
	private Node selectedNode;

	/** The graph's view panel. */
	private ViewPanel view;

	/** The graph's view pipe. Used to listen for node click events. */
	private ViewerPipe vp;

	/** The text area where node content will be shown. */
	private JTextArea text;

	/**
	 * Initialize a graph panel.
	 * 
	 * @param w
	 *            The window this panel is part of.
	 */
	public GraphPanel(final Window w) {
		super(JSplitPane.VERTICAL_SPLIT, true);
		this.window = w;
		registerObserver(this);

		graphPane = new JScrollPane();
		graphPane.setMinimumSize(new Dimension(2, 2));

		text = new JTextArea();
		text.setLineWrap(true);
		infoPane = new JScrollPane(text);
		// infoPane.setAutoscrolls(false);

		setTopComponent(graphPane);
		setBottomComponent(infoPane);

		// SwingUtilities.invokeLater(new Runnable() {
		// public void run() {
		setDividerLocation(DEFAULT_DIVIDER_LOC);
		setResizeWeight(1);
		// }
		// });
		new GenomeHighlight();
		new ScrollListener(graphPane.getHorizontalScrollBar());
	}

	/**
	 * Write the visual graph representation to a file.
	 * 
	 * @param filePath
	 *            Target path for exporting the file.
	 */
	public final void writeGraph(final String filePath) {
		// new Thread(new Runnable() {
		// public void run() {
		try {
			graph.write(filePath);
			Event.statusBarInfo("Exported graph to: " + filePath);
		} catch (IOException e) {
			Event.statusBarError("Writing the graph went wrong ("
					+ e.getMessage() + ")");
			e.printStackTrace();
		}
		// }
		// });
	}

	/**
	 * Loads a graph into the content scroll pane.
	 * 
	 * @param nodes
	 *            The path to the node file.
	 * @param edges
	 *            The path to the edge file.
	 * @return true iff the graph was loaded successfully.
	 */
	public final boolean loadGraph(final File nodes, final File edges) {
		ProgressDialog pd = new ProgressDialog(window, "Importing graph", true);
		pd.start();
		boolean ret = true;
		// Thread t = new Thread(new Runnable() {
		// public void run() {
		// try {
		// Thread.sleep(500);
		// } catch (InterruptedException e1) {
		// e1.printStackTrace();
		// }
		try {
			DGraph dgraph = Reader.read(nodes.getAbsolutePath(),
					edges.getAbsolutePath());
			NodePlacer.place(dgraph);
			graph = ConvertDGraph.convert(dgraph);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Event.statusBarError(e.getMessage());
		}
		graph.addAttribute("ui.stylesheet",
				"url('src/main/resources/stylesheet.css')");
		// System.setProperty("org.graphstream.ui.renderer",
		// "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		Viewer viewer = new Viewer(graph,
				Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
		viewer.disableAutoLayout();
		view = viewer.addDefaultView(false);
		view.setPreferredSize(VIEW_SIZE);

		vp = viewer.newViewerPipe();
		vp.addViewerListener(new NodeClickListener());

		view.addMouseListener(new ViewMouseListener());
		graphPane.setViewportView(view);
		// graphPane.setMinimumSize(new Dimension(50, 50));
		// graphPane.setPreferredSize(new Dimension(50, 50));
		window.revalidate();
		pd.end();
		graphPane.getVerticalScrollBar().setValue(
				graphPane.getVerticalScrollBar().getMaximum());
		graphPane.getVerticalScrollBar().setValue(
				graphPane.getVerticalScrollBar().getValue() / 2);
		return ret;
	}

	/**
	 * 
	 * @param o
	 *            The observer to add.
	 */
	public final void registerObserver(final NodeSelectionObserver o) {
		observers.add(o);
	}

	/**
	 * 
	 * @param o
	 *            The observer to delete.
	 */
	public final void unregisterObserver(final NodeSelectionObserver o) {
		observers.remove(o);
	}

	/**
	 * 
	 * @param selectedNodeIn
	 *            The node clicked on by the user
	 */
	private void notifyObservers(final Node selectedNodeIn) {
		for (NodeSelectionObserver sgo : observers) {
			sgo.update(selectedNodeIn);
		}
	}

	/**
	 * {@inheritDoc} Changes to the graph graphics based on the selected node.
	 */
	public final void update(final Node newSelectedNode) {
		// text.setText(newSelectedNode.getAttribute("content").toString());

		// infoPane.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
		// JViewport jv = infoPane.getViewport();
		// jv.setViewPosition(new Point(0, 0));

		// TODO change strings to constants (still have to decide in which class
		// to define them)

	}

	public final void selectNode(final Node newSelectedNode) {
		// Restores the old class of the previous selected node if present.
		if (selectedNode != null) {
			selectedNode.setAttribute("ui.class",
					selectedNode.getAttribute("oldclass"));
			selectedNode.removeAttribute("oldclass");
		}

		// Assigns new selected node and stores old ui.class
		selectedNode = newSelectedNode;
		selectedNode.setAttribute("oldclass",
				selectedNode.getAttribute("ui.class"));
		selectedNode.addAttribute("ui.class", "selected");
	}

	/**
	 * Highlights a genome. TODO debug code delete
	 */
	public final void highlight() {
		// text.insert(String.valueOf(graph.getNode(1184).getDegree()), text
		// .getText().length());
		graph.getNode(1184).setAttribute("ui.class", "common");
		graph.getNode(1184).setAttribute("ui.color", 0.25);
		graph.getNode(1183).setAttribute("ui.class", "common");
		graph.getNode(1183).setAttribute("ui.color", 0.5);
		graph.getNode(1256).setAttribute("ui.class", "common");
		graph.getNode(1256).setAttribute("ui.color", 1);
	}

	/**
	 * Unhighlights a genome. TODO debug code delete
	 */
	public final void unHighlight() {
		graph.getNode(1184).setAttribute("ui.class", "highlight");
		graph.getNode(1183).setAttribute("ui.class", "highlight");
		graph.getNode(1256).setAttribute("ui.class", "highlight");
	}

	@Override
	public final String toString() {
		return this.getClass().getName();
	}

	/**
	 * 
	 * @author Maarten
	 * @since 22-5-2015
	 * @version 1.0
	 */
	final class ScrollListener implements AdjustmentListener {

		/**
		 * Initialize the scroll listener and make it observe a given scroll
		 * bar.
		 * 
		 * @param scrollBar
		 *            The scroll bar to observe.
		 */
		private ScrollListener(final JScrollBar scrollBar) {
			scrollBar.addAdjustmentListener(this);
		}

		/** {@inheritDoc} */
		public void adjustmentValueChanged(final AdjustmentEvent e) {
			int type = e.getAdjustmentType();
			if (type == AdjustmentEvent.BLOCK_DECREMENT) {
				System.out.println("Scroll block decr");
			} else if (type == AdjustmentEvent.BLOCK_INCREMENT) {
				System.out.println("Scroll block incr");
			} else if (type == AdjustmentEvent.UNIT_DECREMENT) {
				System.out.println("Scroll unit incr");
			} else if (type == AdjustmentEvent.UNIT_INCREMENT) {
				System.out.println("Scroll unit incr");
			} else if (type == AdjustmentEvent.TRACK) {
				System.out.println("Scroll track");
				System.out.println("e.getValue() == " + e.getValue());
				// TODO (use this for genome location plotting?)
				// seems to always have this adjustment event type :$
				// http://examples.javacodegeeks.com/desktop-java/awt/event/adjustmentlistener-example
			}
		}

	}

	/**
	 * Observer class implementing the GenomeTableObserver interface processing
	 * events for filtering genomes.
	 * 
	 * @author ChakShun
	 * @since 18-05-2015
	 */
	class GenomeHighlight implements GenomeTableObserver {

		/**
		 * Constructor in which it adds itself to the observers for the option
		 * panel.
		 */
		public GenomeHighlight() {
			window.optionPanel().getGenomes().registerObserver(this);
		}

		/** {@inheritDoc} */
		public void update(final GenomeRow genomeRow,
				final boolean genomeFilterChanged,
				final boolean genomeHighlightChanged) {
			if (genomeHighlightChanged && genomeRow.isVisible()) {
				if (genomeRow.isHighlighted()) {
					highlight();
				} else {
					unHighlight();
				}
			}

		}
	}

	/**
	 * A viewer listener notifying all node selection observers when a node in
	 * the currently loaded graph gets clicked.
	 * 
	 * @see NodeSelectionObserver
	 * 
	 * @author Maarten
	 * @since 18-5-2015
	 * @version 1.0
	 */
	class NodeClickListener implements ViewerListener {

		/**
		 * {@inheritDoc}
		 */
		public void viewClosed(final String viewName) {
		}

		/**
		 * {@inheritDoc}
		 */
		public void buttonReleased(final String id) {
			Event.statusBarMid("Selected node: " + id);
			notifyObservers(graph.getNode(id));
		}

		/**
		 * {@inheritDoc}
		 */
		public void buttonPushed(final String id) {
			selectNode(graph.getNode(id));
		}
	}

	/**
	 * A mouse listener pumping the viewer pipe each time the mouse is pressed
	 * or released. This makes sure that the NodeClickListener receives its
	 * click events immediately when a node is clicked.
	 * 
	 * @see NodeClickListener
	 * @see ViewerPipe
	 * 
	 * @author Maarten
	 * @since 18-5-2015
	 * @version 1.0
	 */
	class ViewMouseListener implements MouseListener {

		/** {@inheritDoc} */
		public void mouseReleased(final MouseEvent e) {
			vp.pump();
		}

		/** {@inheritDoc} */
		public void mousePressed(final MouseEvent e) {
			vp.pump();
		}

		/** {@inheritDoc} */
		public void mouseExited(final MouseEvent e) {
		}

		/** {@inheritDoc} */
		public void mouseEntered(final MouseEvent e) {
		}

		/** {@inheritDoc} */
		public void mouseClicked(final MouseEvent e) {
		}
	}

}