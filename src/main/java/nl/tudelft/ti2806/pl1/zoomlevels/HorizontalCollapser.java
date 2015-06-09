package nl.tudelft.ti2806.pl1.zoomlevels;

import java.util.LinkedList;
import java.util.Queue;

import org.graphstream.graph.BreadthFirstIterator;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.Graphs;

/**
 * @author Justin, Marissa
 * @since 21-05-2015
 */
public final class HorizontalCollapser {

	/**
	 * 
	 */
	private HorizontalCollapser() {

	}

	/**
	 * Collapses the graph horizontally if the content size does not surpass the
	 * maximum content size.
	 * 
	 * @param graph
	 *            Graph we want to collapse on.
	 * @return Horizontally collapsed graph.
	 */
	public static Graph horizontalCollapse(final Graph graph) {
		Graph g = Graphs.merge(graph);
		return collapseGraph(g);
	}

	/**
	 * Collapses the graph horizontally if the content size does not surpass the
	 * maximum content size.
	 * 
	 * @param graph
	 *            Graph we want to collapse on.
	 * @return Horizontally collapsed graph
	 */
	private static Graph collapseGraph(Graph graph) {
		Node start = graph.getNode("-2");
		BreadthFirstIterator<Node> it = new BreadthFirstIterator<Node>(start);
		Queue<Node> q = new LinkedList<Node>();
		q.add(start);
		while (it.hasNext()) {
			Node node = it.next();
			q.add(node);
		}
		while (!q.isEmpty()) {
			Node node = q.remove();
			graph = collapseNodes(graph, node);
		}
		return graph;
	}

	/**
	 * Collapses a two nodes if their combined content size does not surpass the
	 * maximum content size.
	 * 
	 * @param graph
	 *            The graph.
	 * @param node
	 *            The node in which the other node will be collapsed.
	 * @return The graph with the nodes collapsed or not.
	 */
	private static Graph collapseNodes(final Graph graph, final Node node) {
		if (node.getInDegree() == 1) {
			Node prev = node.getEnteringEdgeIterator().next().getNode0();
			if (prev.getOutDegree() == 1 && !prev.getId().equals("-2")) {
				mergeAttributes(prev, node);
				for (Edge edge : prev.getEnteringEdgeSet()) {
					if (edge != null) {
						String id = edge.getId();
						graph.addEdge(id + " " + node.getId(), edge.getNode0(),
								node, true);
					}
				}
				graph.removeNode(prev);
			}
		}
		return graph;
	}

	/**
	 * @param node
	 *            The node
	 * @return The size of the content of the node.
	 */
	private static int getContentSize(final Node node) {
		int length = 0;
		String content = node.getAttribute("ui.label");
		if (content.matches("\\d+")) {
			length = Integer.parseInt(content);
		} else {
			length = content.length();
		}
		return length;
	}

	/**
	 * Merges the attributes of two nodes.
	 * 
	 * @param from
	 *            The node from which the attributes are taken
	 * @param to
	 *            The node into which the merged attributes are stored
	 */
	private static void mergeAttributes(final Node from, final Node to) {
		to.setAttribute("inNodes", from.getAttribute("inNodes"));
		to.setAttribute("start", from.getAttribute("start"));
		int combinedlength = getContentSize(from) + getContentSize(to);
		to.setAttribute("ui.label", combinedlength + "");
	}
}