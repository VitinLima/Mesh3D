package org.vitinho.mesh3d;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.io.FileInputStream;

import java.util.ArrayList;
import java.util.Collections;

public class StlFile{
    private ArrayList<Node> _nodes;
    private ArrayList<Edge> _edges;
    private ArrayList<Face> _faces;

    private ArrayList<ArrayList<Float>> _unitVectors;
    private ArrayList<NamedSelection> _namedSelections;

    private VisualizationPanel _visualizationPanel;
    private SolidOptionsPanel _solidOptionsPanel;
    private JPanel _solidPanel;

    public static void main(String[] args) throws Exception {
        StlFile solid = new StlFile();
        //solid.loadFile("owl.stl");
        solid.loadFile("square.stl");//System.out.println("Area of face is " + solid.calculateAreaOfFace(0));
        /*System.out.println("File loaded succesfully.");
        System.out.println("Number of faces is " + solid.getNumberOfFaces());
        System.out.println("Number of edges is " + solid.getNumberOfEdges());
        System.out.println("Number of nodes is " + solid.getNumberOfNodes());*/

        JFrame mainFrame = new JFrame();
        mainFrame.setLayout(new FlowLayout());
        //mainFrame.add(new optionsPanel(solid));
        mainFrame.add(solid.getSolidPanel());
        mainFrame.pack();
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setVisible(true);
    }
    public void loadFile(String fileName) throws Exception {
        System.out.println("Loading file " + fileName);
        try (FileInputStream fis = new FileInputStream(fileName)) {
          System.out.println("File description:");
          for(int i = 0; i < 80 && fis.available() > 0; i++){
            System.out.print((char)fis.read());
          }
          System.out.println("");

          int numberOfFaces = 0;
          for(int i = 0; i < 4 && fis.available() > 0; i++){
            numberOfFaces |= fis.read()<<(8*i);
          }

          _faces = new ArrayList<Face>(numberOfFaces);
          _unitVectors = new ArrayList<ArrayList<Float>>(numberOfFaces);
          _nodes = new ArrayList<Node>(3*numberOfFaces);
          for(int i = 0; i < numberOfFaces && fis.available() > 0; i++){
            _unitVectors.add(new ArrayList<Float>(3));
            for(int j = 0; j < 3; j++){
              int readBytesAsInteger = 0;
              for(int k = 0; k < 4; k++){
                readBytesAsInteger |= fis.read()<<(8*k);
              }
              _unitVectors.get(i).add(Float.intBitsToFloat(readBytesAsInteger));
            }

            _faces.add(new Face());
            for(int j = 0; j < 3; j++){
              Node node = new Node();
              for(int k = 0; k < 3; k++){
                int readBytesAsInteger = 0;
                for(int l = 0; l < 4; l++){
                  readBytesAsInteger |= fis.read()<<(8*l);
                }
                node.set(k, Float.intBitsToFloat(readBytesAsInteger));
              }
              if(!_nodes.contains(node))
                _nodes.add(node);
              _faces.get(i).add(_nodes.indexOf(node));
            }

            fis.read();
            fis.read();
          }
        }

        _nodes.trimToSize();
        orderFaces();
        createEdges();
        
        _visualizationPanel = new VisualizationPanel(_nodes,_edges,_faces);
        _solidOptionsPanel = new SolidOptionsPanel(_visualizationPanel);
        _solidPanel = new JPanel(new GridLayout(2,1)){{
            add(_visualizationPanel);
            add(_solidOptionsPanel);
        }};
    }
    private void orderFaces(){
        for(Face face : _faces){
            if(calculateAreaOfFace(face) < 0){
                face.set(0,face.get(0)+face.get(1));
                face.set(1,face.get(0)-face.get(1));
                face.set(0,face.get(0)-face.get(1));
                //if(calculateAreaOfFace(face) < 0) System.out.println("Error in face " + i);
            }
        }
    }
    public float calculateAreaOfFace(Face face){
        float[] point_1 = new float[]{_nodes.get(face.get(0)).get(0),_nodes.get(face.get(0)).get(1),_nodes.get(face.get(0)).get(2)};
        float[] point_2 = new float[]{_nodes.get(face.get(1)).get(0),_nodes.get(face.get(1)).get(1),_nodes.get(face.get(1)).get(2)};
        float[] point_3 = new float[]{_nodes.get(face.get(2)).get(0),_nodes.get(face.get(2)).get(1),_nodes.get(face.get(2)).get(2)};
        point_2 = new float[]{point_2[0]-point_1[0],point_2[1]-point_1[1],point_2[2]-point_1[2]};
        point_3 = new float[]{point_3[0]-point_1[0],point_3[1]-point_1[1],point_3[2]-point_1[2]};
        float[] crossMultiplication = new float[]{
            point_2[1]*point_3[2] - point_2[2]*point_3[1],
            point_2[2]*point_3[0] - point_2[0]*point_3[2],
            point_2[0]*point_3[1] - point_2[1]*point_3[0]
        };
        return (float)Math.sqrt(crossMultiplication[0]*crossMultiplication[0] + crossMultiplication[1]*crossMultiplication[1] + crossMultiplication[2]*crossMultiplication[2])/2.0f;
    }
    private void createEdges(){
        _edges = new ArrayList<Edge>(3*_faces.size());

        for(Face face : _faces){
            Edge edge;
            edge = new Edge(){{
                add(face.get(0));
                add(face.get(1));
            }};
            if(!_edges.contains(edge)){
                _edges.add(edge);
            }
            face.addEdge(_edges.indexOf(edge));
            edge = new Edge(){{
                add(face.get(1));
                add(face.get(2));
            }};
            if(!_edges.contains(edge)){
                _edges.add(edge);
            }
            face.addEdge(_edges.indexOf(edge));
            edge = new Edge(){{
                add(face.get(2));
                add(face.get(0));
            }};
            if(!_edges.contains(edge)){
                _edges.add(edge);
            }
            face.addEdge(_edges.indexOf(edge));
        }
    }
    private class Face implements Comparable<Face>{
        private ArrayList<Integer> _nodeIdxs;
        private ArrayList<Integer> _edgeIdxs;
        public float distanceToCamera;
        public Color defaultColor;
        public Color color;
        public boolean isDrawable;
        public boolean isVisible;
        private boolean _isSelected;
        public Face(){
            _nodeIdxs = new ArrayList<Integer>(3);
            _edgeIdxs = new ArrayList<Integer>(3);
            defaultColor = Color.gray;
            color = Color.gray;
            isDrawable = true;
            _isSelected = false;
        }
        public int get(int idx){
            return _nodeIdxs.get(idx);
        }
        public void add(int idx){
            _nodeIdxs.add(idx);
        }
        public void set(int idx, int idxNode){
            _nodeIdxs.set(idx, idxNode);
        }
        public int getEdge(int idx){
            return _edgeIdxs.get(idx);
        }
        public void addEdge(int idx){
            _edgeIdxs.add(idx);
        }
        public void update(float[] cameraPosition, ArrayList<Node> nodes){
            //distanceToCamera = (float)Math.sqrt(nodes.get(_nodeIdxs.get(0)).getRelativePosition(0)*nodes.get(_nodeIdxs.get(0)).getRelativePosition(0) + nodes.get(_nodeIdxs.get(0)).getRelativePosition(1)*nodes.get(_nodeIdxs.get(0)).getRelativePosition(1) + nodes.get(_nodeIdxs.get(0)).getRelativePosition(2)*nodes.get(_nodeIdxs.get(0)).getRelativePosition(2));
            distanceToCamera = nodes.get(_nodeIdxs.get(0)).getRelativePosition(2);
            distanceToCamera += nodes.get(_nodeIdxs.get(1)).getRelativePosition(2);
            distanceToCamera += nodes.get(_nodeIdxs.get(2)).getRelativePosition(2);
            //distanceToCamera /= 3.0f;
            isVisible = nodes.get(_nodeIdxs.get(0)).isVisible || nodes.get(_nodeIdxs.get(1)).isVisible || nodes.get(_nodeIdxs.get(2)).isVisible;
        }
        @Override
        public int compareTo(Face face){
            if(distanceToCamera < face.distanceToCamera) return 1;
            if(distanceToCamera > face.distanceToCamera) return -1;
            return 0;
        }
        public boolean containsDrawPosition(int x, int y, ArrayList<Node> nodes){
            Polygon polygon = new Polygon(new int[]{
                nodes.get(_nodeIdxs.get(0)).getDrawPosition(0),
                nodes.get(_nodeIdxs.get(1)).getDrawPosition(0),
                nodes.get(_nodeIdxs.get(2)).getDrawPosition(0),
            }, new int[]{
                nodes.get(_nodeIdxs.get(0)).getDrawPosition(1),
                nodes.get(_nodeIdxs.get(1)).getDrawPosition(1),
                nodes.get(_nodeIdxs.get(2)).getDrawPosition(1)
            }, 3);
            return polygon.contains(x, y);
        }
        public boolean isSelected(){
            return _isSelected;
        }
        public void setSelected(boolean isSelected){
            _isSelected = isSelected;
            color = _isSelected ? Color.green : defaultColor;
        }
    }
    private class Edge{
        private ArrayList<Integer> _nodeIdxs;
        public Color defaultColor;
        public Color color;
        public boolean isDrawable;
        public boolean isVisible;
        private boolean _isSelected;
        public Edge(){
            _nodeIdxs = new ArrayList<Integer>(2);
            defaultColor = Color.blue;
            color = Color.blue;
            isDrawable = true;
            _isSelected = false;
        }
        public int get(int idx){
            return _nodeIdxs.get(idx);
        }
        public void add(int idx){
            _nodeIdxs.add(idx);
        }
        public boolean equals(Edge edge){
            boolean condition_1 = edge.get(0) == _nodeIdxs.get(0) && edge.get(1) == _nodeIdxs.get(1);
            boolean condition_2 = edge.get(0) == _nodeIdxs.get(1) && edge.get(1) == _nodeIdxs.get(0);
            return condition_1 || condition_2;
        }
        public boolean isSelected(){
            return _isSelected;
        }
        public void setSelected(boolean isSelected){
            _isSelected = isSelected;
            color = _isSelected ? Color.green : defaultColor;
        }
    }
    private class Node{
        private float[] _position;
        private float[] _relativePosition;
        private int[] _drawPosition;
        public float size;
        public int drawSize;
        public Color defaultColor;
        public Color color;
        public boolean isDrawable;
        public boolean isVisible;
        private boolean _isSelected;
        public Node(){
            _position = new float[3];
            _relativePosition = new float[3];
            _drawPosition = new int[2];
            size = 2.0f;
            defaultColor = Color.darkGray;
            color = Color.darkGray;
            isDrawable = true;
            _isSelected = false;
        }
        public float get(int idx){
            return _position[idx];
        }
        public void set(int idx, float idxPosition){
            _position[idx] = idxPosition;
        }
        public float getRelativePosition(int idx){
            return _relativePosition[idx];
        }
        public int getDrawPosition(int idx){
            return _drawPosition[idx];
        }
        public void update(float[] cameraPosition, float[] cameraOrientation, float FOVAngle, JPanel drawingPanel){
            _relativePosition[0] = _position[0] - cameraPosition[0];
            _relativePosition[1] = _position[1] - cameraPosition[1];
            _relativePosition[2] = _position[2] - cameraPosition[2];
            _relativePosition = VisualizationPanel.rotateZ(cameraOrientation[2], _relativePosition);
            _relativePosition = VisualizationPanel.rotateY(-cameraOrientation[1], _relativePosition);
            _relativePosition = VisualizationPanel.rotateX(cameraOrientation[0], _relativePosition);
            
            float angleY = (float)Math.atan2((double)_relativePosition[0],(double)_relativePosition[2]);
            float angleX = (float)Math.atan2((double)_relativePosition[1],(double)_relativePosition[2]);
            _drawPosition[0] = -Math.round(angleY/FOVAngle*(float)drawingPanel.getSize().getWidth()/2.0f);
            _drawPosition[1] = -Math.round(angleX/FOVAngle*(float)drawingPanel.getSize().getHeight()/2.0f);

            drawSize = Math.abs(Math.round((float)Math.atan2((double)size,(double)_relativePosition[2])/FOVAngle*(float)drawingPanel.getSize().getWidth()/2.0f));

            isVisible = Math.round(drawingPanel.getSize().getWidth()/2.0f) + drawSize - Math.abs(_drawPosition[0]) > 0;
            isVisible = isVisible && Math.round(drawingPanel.getSize().getHeight()/2.0f) + drawSize - Math.abs(_drawPosition[1]) > 0;
            isVisible = isVisible && _relativePosition[2] > 0;

            _drawPosition[0] += Math.round(drawingPanel.getSize().getWidth()/2.0f);
            _drawPosition[1] += Math.round(drawingPanel.getSize().getHeight()/2.0f);
        }
        public boolean equals(Node node){
            return node.get(0) == _position[0] && node.get(1) == _position[1] && node.get(2) == _position[2];
        }
        public boolean isSelected(){
            return _isSelected;
        }
        public void setSelected(boolean isSelected){
            _isSelected = isSelected;
            color = _isSelected ? Color.green : defaultColor;
        }
    }
    private static class VisualizationPanel extends JPanel implements MouseListener,MouseMotionListener,MouseWheelListener,KeyListener{
        private float[] _cameraPosition;
        private float[] _cameraOrientation;
        private float[] _cameraXAxis;
        private float[] _cameraYAxis;
        private float[] _cameraZAxis;

        private ArrayList<Node> _nodes;
        private ArrayList<Edge> _edges;
        private ArrayList<Face> _faces;

        public boolean drawNodes;
        public boolean drawEdges;
        public boolean drawFaces;

        public boolean selectFaces;
        public boolean selectEdges;
        public boolean selectNodes;

        private static final int _minClickSelectionDistance = 10;
        private int _pressedCoordinateX, _pressedCoordinateY;
        private static final float _spree = 5.0f;
        private static final float _rotationSpree = (float)Math.toRadians(10.0);
        private static final float _FOVAngle = (float)Math.toRadians(30.0);
        private ArrayList<Character> _pressedKeys;

        public VisualizationPanel(ArrayList<Node> nodes, ArrayList<Edge> edges, ArrayList<Face> faces){
            super(new FlowLayout());
            super.setPreferredSize(new Dimension(500,300));
            super.setBackground(Color.black);
            super.setFocusable(true);

            _nodes = nodes;
            _edges = edges;
            _faces = faces;

            _cameraPosition = new float[3];
            _cameraOrientation = new float[3];
            _cameraXAxis = new float[3];
            _cameraYAxis = new float[3];
            _cameraZAxis = new float[3];

            drawNodes = !false;
            drawEdges = true;
            drawFaces = true;

            selectFaces = true;
            selectEdges = false;
            selectNodes = false;

            _pressedKeys = new ArrayList<Character>(12);

            fitToView();

            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
            addKeyListener(this);
        }
        public void paintComponent(Graphics g){
            super.paintComponent(g);
            for(Node node : _nodes){
                node.update(_cameraPosition, _cameraOrientation, _FOVAngle, this);
            }
            for(Edge edge : _edges){
                edge.isVisible = _nodes.get(edge.get(0)).isVisible || _nodes.get(edge.get(1)).isVisible;
            }
            for(Face face : _faces){
                face.update(_cameraPosition, _nodes);
            }
            Collections.sort(_faces);
            for(Face face : _faces){
                drawFace(face, g);
            }
        }
        private void drawFace(Face face, Graphics g){
            g.setColor(face.color);
            if(drawFaces && face.isDrawable && face.isVisible){
                int[] point_x = new int[]{
                    _nodes.get(face.get(0)).getDrawPosition(0),
                    _nodes.get(face.get(1)).getDrawPosition(0),
                    _nodes.get(face.get(2)).getDrawPosition(0),
                };
                int[] point_y = new int[]{
                    _nodes.get(face.get(0)).getDrawPosition(1),
                    _nodes.get(face.get(1)).getDrawPosition(1),
                    _nodes.get(face.get(2)).getDrawPosition(1),
                };
                g.fillPolygon(point_x, point_y, 3);
            }

            for(int i = 0; i < 3; i++){
                drawEdge(_edges.get(face.getEdge(i)), g);
            }

            for(int i = 0; i < 3; i++){
                drawNode(_nodes.get(face.get(i)), g);
            }
        }
        private void drawEdge(Edge edge, Graphics g){
            if(drawEdges && edge.isDrawable && edge.isVisible){
                g.setColor(edge.color);
                g.drawLine(_nodes.get(edge.get(0)).getDrawPosition(0), _nodes.get(edge.get(0)).getDrawPosition(1), _nodes.get(edge.get(1)).getDrawPosition(0), _nodes.get(edge.get(1)).getDrawPosition(1));
            }
        }
        private void drawNode(Node node, Graphics g){
            if(drawNodes && node.isDrawable && node.isVisible){
                g.setColor(node.color);
                g.fillOval(node.getDrawPosition(0)-node.drawSize/2,node.getDrawPosition(1)-node.drawSize/2,node.drawSize,node.drawSize);
            }
        }
        public void fitToView(){
            if(_nodes.isEmpty()){
                _cameraPosition = new float[]{0.0f, 0.0f, 0.0f};
                _cameraOrientation = new float[]{0.0f, 0.0f, 0.0f};
                updateCameraAxis();
                return;
            }
            float minX = _nodes.get(0).get(0);
            float minY = _nodes.get(0).get(1);
            float minZ = _nodes.get(0).get(2);
            float maxX = _nodes.get(0).get(0);
            float maxY = _nodes.get(0).get(1);
            float maxZ = _nodes.get(0).get(2);
            for(int i = 1; i < _nodes.size(); i++){
                if(_nodes.get(i).get(0) < minX) minX = _nodes.get(i).get(0);
                if(_nodes.get(i).get(1) < minY) minY = _nodes.get(i).get(1);
                if(_nodes.get(i).get(2) < minZ) minZ = _nodes.get(i).get(2);
                if(_nodes.get(i).get(0) > maxX) maxX = _nodes.get(i).get(0);
                if(_nodes.get(i).get(1) > maxY) maxY = _nodes.get(i).get(1);
                if(_nodes.get(i).get(2) > maxZ) maxZ = _nodes.get(i).get(2);
            }
            float dx = maxX - minX;
            float dy = maxY - minY;
            float dz = maxZ - minZ;
            float meanX = (maxX + minX)/2.0f;
            float meanY = (maxY + minY)/2.0f;
            float meanZ = (maxZ + minZ)/2.0f;
            float scale = (float)Math.sqrt(dx*dx + dy*dy + dz*dz)/1.5f;
            _cameraPosition = new float[]{meanX + scale, meanY + scale, meanZ + scale};
            _cameraOrientation = new float[]{(float)Math.toRadians(125.0), (float)Math.toRadians(10.0), (float)Math.toRadians(-50.0)};
            updateCameraAxis();
            repaint();
        }
        public ArrayList<Integer> getSelectedFaces(){
            ArrayList<Integer> selectedFaces = new ArrayList<Integer>(_faces.size());
            for(int idx = 0; idx < _faces.size(); idx++){
                if(_faces.get(idx).isSelected()){
                    selectedFaces.add(idx);
                }
            }
            return selectedFaces;
        }
        public ArrayList<Integer> getSelectedEdges(){
            ArrayList<Integer> selectedEdges = new ArrayList<Integer>(_edges.size());
            for(int idx = 0; idx < _edges.size(); idx++){
                if(_edges.get(idx).isSelected()){
                    selectedEdges.add(idx);
                }
            }
            return selectedEdges;
        }
        public ArrayList<Integer> getSelectedNodes(){
            ArrayList<Integer> selectedNodes = new ArrayList<Integer>(_nodes.size());
            for(int idx = 0; idx < _nodes.size(); idx++){
                if(_nodes.get(idx).isSelected()){
                    selectedNodes.add(idx);
                }
            }
            return selectedNodes;
        }

        @Override
        public void mouseClicked(MouseEvent e){
            //System.out.println("Mouse clicked!");
            if(!isFocusOwner()){
                requestFocus();
                return;
            }
            Collections.reverse(_faces);
            if(selectFaces){
                boolean flg = true;
                for(int idx = 0; idx < _faces.size() && flg; idx++){
                    if(_faces.get(idx).containsDrawPosition(e.getX(), e.getY(), _nodes)){
                        _faces.get(idx).setSelected(!_faces.get(idx).isSelected());
                        flg = false;
                    }
                }
            }
            Collections.reverse(_faces);
            repaint();
        }

        @Override
        public void mouseEntered(MouseEvent e){
            //System.out.println("Mouse entered!");
        }

        @Override
        public void mouseExited(MouseEvent e){
            //System.out.println("Mouse exited!");
        }

        @Override
        public void mousePressed(MouseEvent e){
            //System.out.println("Mouse pressed!");
            _pressedCoordinateX = e.getX();
            _pressedCoordinateY = e.getY();
        }

        @Override
        public void mouseReleased(MouseEvent e){
            //System.out.println("Mouse released!");
        }

        @Override
        public void mouseDragged(MouseEvent e){
            //System.out.println("Mouse dragged!");
            if(SwingUtilities.isLeftMouseButton(e)){
            }else if(SwingUtilities.isRightMouseButton(e)){
            }else if(SwingUtilities.isMiddleMouseButton(e)){
            }
            _pressedCoordinateX = e.getX();
            _pressedCoordinateY = e.getY();
            repaint();
        }

        @Override
        public void mouseMoved(MouseEvent e){
            //System.out.println("Mouse moved!");
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e){
            //System.out.println("Mouse wheel moved!");
            _cameraPosition[0] += _cameraZAxis[0]*(float)e.getPreciseWheelRotation();
            _cameraPosition[1] += _cameraZAxis[1]*(float)e.getPreciseWheelRotation();
            _cameraPosition[2] += _cameraZAxis[2]*(float)e.getPreciseWheelRotation();
            System.out.println("Camera orientation " + Math.toDegrees(_cameraOrientation[0]) + " " + Math.toDegrees(_cameraOrientation[1]) + " " + Math.toDegrees(_cameraOrientation[2]));
            System.out.println("Movement direction " + _cameraZAxis[0] + " " + _cameraZAxis[1] + " " + _cameraZAxis[2]);
            System.out.println("Camera position " + _cameraPosition[0] + " " + _cameraPosition[1] + " " + _cameraPosition[2] + "\n");
            repaint();
        }

        @Override
        public void keyPressed(KeyEvent e){
            //System.out.println("Key pressed! " + e.getKeyChar());
            if(!_pressedKeys.contains(e.getKeyChar())){
                _pressedKeys.add(e.getKeyChar());
            }
            for(char pressedKey : _pressedKeys){
                switch(pressedKey){
                    case 'w' -> {
                      _cameraPosition[0] += _cameraZAxis[0]*_spree;
                      _cameraPosition[1] += _cameraZAxis[1]*_spree;
                      _cameraPosition[2] += _cameraZAxis[2]*_spree;
                }
                    case 's' -> {
                      _cameraPosition[0] -= _cameraZAxis[0]*_spree;
                      _cameraPosition[1] -= _cameraZAxis[1]*_spree;
                      _cameraPosition[2] -= _cameraZAxis[2]*_spree;
                }
                    case 'a' -> {
                      _cameraPosition[0] += _cameraXAxis[0]*_spree;
                      _cameraPosition[1] += _cameraXAxis[1]*_spree;
                      _cameraPosition[2] += _cameraXAxis[2]*_spree;
                }
                    case 'd' -> {
                      _cameraPosition[0] -= _cameraXAxis[0]*_spree;
                      _cameraPosition[1] -= _cameraXAxis[1]*_spree;
                      _cameraPosition[2] -= _cameraXAxis[2]*_spree;
                }
                    case ' ' -> {
                      _cameraPosition[0] += _cameraYAxis[0]*_spree;
                      _cameraPosition[1] += _cameraYAxis[1]*_spree;
                      _cameraPosition[2] += _cameraYAxis[2]*_spree;
                }
                    case 'c' -> {
                      _cameraPosition[0] -= _cameraYAxis[0]*_spree;
                      _cameraPosition[1] -= _cameraYAxis[1]*_spree;
                      _cameraPosition[2] -= _cameraYAxis[2]*_spree;
                }
                    case 'q' -> {
                      _cameraZAxis = rotateAroundAxis(_rotationSpree, _cameraYAxis, _cameraZAxis);
                      _cameraXAxis = rotateAroundAxis(_rotationSpree, _cameraYAxis, _cameraXAxis);
                      updateCameraOrientation();
                }
                    case 'e' -> {
                      _cameraZAxis = rotateAroundAxis(-_rotationSpree, _cameraYAxis, _cameraZAxis);
                      _cameraXAxis = rotateAroundAxis(-_rotationSpree, _cameraYAxis, _cameraXAxis);
                      updateCameraOrientation();
                }
                    case 'r' -> {
                      _cameraYAxis = rotateAroundAxis(-_rotationSpree, _cameraXAxis, _cameraYAxis);
                      _cameraZAxis = rotateAroundAxis(-_rotationSpree, _cameraXAxis, _cameraZAxis);
                      updateCameraOrientation();
                }
                    case 'f' -> {
                      _cameraYAxis = rotateAroundAxis(_rotationSpree, _cameraXAxis, _cameraYAxis);
                      _cameraZAxis = rotateAroundAxis(_rotationSpree, _cameraXAxis, _cameraZAxis);
                      updateCameraOrientation();
                }
                    case 'z' -> {
                      _cameraXAxis = rotateAroundAxis(-_rotationSpree, _cameraZAxis, _cameraXAxis);
                      _cameraYAxis = rotateAroundAxis(-_rotationSpree, _cameraZAxis, _cameraYAxis);
                      updateCameraOrientation();
                }
                    case 'x' -> {
                      _cameraXAxis = rotateAroundAxis(_rotationSpree, _cameraZAxis, _cameraXAxis);
                      _cameraYAxis = rotateAroundAxis(_rotationSpree, _cameraZAxis, _cameraYAxis);
                      updateCameraOrientation();
                }
                    default -> {
                }
                }
                repaint();
            }
        }

        @Override
        public void keyReleased(KeyEvent e){
            //System.out.println("Key released! " + e.getKeyChar());
            _pressedKeys.remove(_pressedKeys.indexOf(e.getKeyChar()));
        }

        @Override
        public void keyTyped(KeyEvent e){
            //System.out.println("Key typed! " + e.getKeyChar());
        }
        private void updateCameraAxis(){
            _cameraXAxis[0] = 1.0f;
            _cameraXAxis[1] = 0.0f;
            _cameraXAxis[2] = 0.0f;
            _cameraYAxis[0] = 0.0f;
            _cameraYAxis[1] = 1.0f;
            _cameraYAxis[2] = 0.0f;
            _cameraZAxis[0] = 0.0f;
            _cameraZAxis[1] = 0.0f;
            _cameraZAxis[2] = 1.0f;
            _cameraYAxis = rotateX(-_cameraOrientation[0], _cameraYAxis);
            _cameraZAxis = rotateX(-_cameraOrientation[0], _cameraZAxis);
            _cameraXAxis = rotateX(-_cameraOrientation[0], _cameraXAxis);
            _cameraXAxis = rotateY(_cameraOrientation[1], _cameraXAxis);
            _cameraYAxis = rotateY(_cameraOrientation[1], _cameraYAxis);
            _cameraZAxis = rotateY(_cameraOrientation[1], _cameraZAxis);
            _cameraXAxis = rotateZ(-_cameraOrientation[2], _cameraXAxis);
            _cameraYAxis = rotateZ(-_cameraOrientation[2], _cameraYAxis);
            _cameraZAxis = rotateZ(-_cameraOrientation[2], _cameraZAxis);
        }
        private void updateCameraOrientation(){
            _cameraOrientation[2] = (float)Math.atan2((double)_cameraXAxis[1],(double)_cameraXAxis[0]);
            _cameraOrientation[1] = (float)Math.atan2((double)_cameraXAxis[2],Math.sqrt(_cameraXAxis[0]*_cameraXAxis[0] + _cameraXAxis[1]*_cameraXAxis[1]));
            float[] axisz = new float[]{_cameraZAxis[0], _cameraZAxis[1], _cameraZAxis[2]};
            axisz = rotateZ(_cameraOrientation[2], axisz);
            axisz = rotateY(-_cameraOrientation[1], axisz);
            _cameraOrientation[0] = (float)Math.atan2(-(double)axisz[1],(double)axisz[2]);updateCameraAxis();
        }
        private static float[] rotateX(float angle, float[] vector){
            float angleCos = (float)Math.cos(angle);
            float angleSin = (float)Math.sin(angle);
            return new float[]{
                vector[0],
                vector[1]*angleCos + vector[2]*angleSin,
                vector[2]*angleCos - vector[1]*angleSin
            };
        }
        private static float[] rotateY(float angle, float[] vector){
            float angleCos = (float)Math.cos(angle);
            float angleSin = (float)Math.sin(angle);
            return new float[]{
                vector[0]*angleCos - vector[2]*angleSin,
                vector[1],
                vector[2]*angleCos + vector[0]*angleSin
            };
        }
        private static float[] rotateZ(float angle, float[] vector){
            float angleCos = (float)Math.cos(angle);
            float angleSin = (float)Math.sin(angle);
            return new float[]{
                vector[0]*angleCos + vector[1]*angleSin,
                vector[1]*angleCos - vector[0]*angleSin,
                vector[2]
            };
        }
        public static float[] rotateAroundAxis(float angle, float[] rotation, float[] rotating){
            float angleBy2Sin = (float)Math.sin(angle/2);
            float angleBy2Cos = (float)Math.cos(angle/2);
            float[] quaternionRotation = new float[]{angleBy2Cos, -angleBy2Sin*rotation[0], -angleBy2Sin*rotation[1], -angleBy2Sin*rotation[2]};
            float[] quaternionRotating = new float[]{0.0f, rotating[0], rotating[1], rotating[2]};
            quaternionRotating = new float[]{
                quaternionRotating[0]*quaternionRotation[0] - quaternionRotating[1]*quaternionRotation[1] - quaternionRotating[2]*quaternionRotation[2] - quaternionRotating[3]*quaternionRotation[3],
                quaternionRotating[0]*quaternionRotation[1] + quaternionRotating[1]*quaternionRotation[0] + quaternionRotating[2]*quaternionRotation[3] - quaternionRotating[3]*quaternionRotation[2],
                quaternionRotating[0]*quaternionRotation[2] + quaternionRotating[2]*quaternionRotation[0] + quaternionRotating[3]*quaternionRotation[1] - quaternionRotating[1]*quaternionRotation[3],
                quaternionRotating[0]*quaternionRotation[3] + quaternionRotating[3]*quaternionRotation[0] + quaternionRotating[1]*quaternionRotation[2] - quaternionRotating[2]*quaternionRotation[1]
            };
            quaternionRotation = new float[]{angleBy2Cos, angleBy2Sin*rotation[0], angleBy2Sin*rotation[1], angleBy2Sin*rotation[2]};
            quaternionRotating = new float[]{
                quaternionRotation[0]*quaternionRotating[0] - quaternionRotation[1]*quaternionRotating[1] - quaternionRotation[2]*quaternionRotating[2] - quaternionRotation[3]*quaternionRotating[3],
                quaternionRotation[0]*quaternionRotating[1] + quaternionRotation[1]*quaternionRotating[0] + quaternionRotation[2]*quaternionRotating[3] - quaternionRotation[3]*quaternionRotating[2],
                quaternionRotation[0]*quaternionRotating[2] + quaternionRotation[2]*quaternionRotating[0] + quaternionRotation[3]*quaternionRotating[1] - quaternionRotation[1]*quaternionRotating[3],
                quaternionRotation[0]*quaternionRotating[3] + quaternionRotation[3]*quaternionRotating[0] + quaternionRotation[1]*quaternionRotating[2] - quaternionRotation[2]*quaternionRotating[1]
            };
            return new float[]{quaternionRotating[1], quaternionRotating[2], quaternionRotating[3]};
        }
    }
    private class SolidOptionsPanel extends JPanel{
        VisualizationPanel _visualizationPanel;
        public SolidOptionsPanel(VisualizationPanel visualizationPanel){
            super(new GridLayout(0,3));
            //setPreferredSize(new Dimension(150,40));

            _visualizationPanel = visualizationPanel;
            add(new JCheckBox("Show faces", _visualizationPanel.drawFaces){{
                setPreferredSize(new Dimension(50,20));
                addActionListener((ActionEvent e) -> {
                  _visualizationPanel.drawFaces = isSelected();
                  _visualizationPanel.repaint();
                });
            }});
            add(new JCheckBox("Show edges", _visualizationPanel.drawEdges){{
                setPreferredSize(new Dimension(50,20));
                addActionListener((ActionEvent e) -> {
                  _visualizationPanel.drawEdges = isSelected();
                  _visualizationPanel.repaint();
                });
            }});
            add(new JCheckBox("Show nodes", _visualizationPanel.drawNodes){{
                setPreferredSize(new Dimension(50,20));
                addActionListener((ActionEvent e) -> {
                  _visualizationPanel.drawNodes = isSelected();
                  _visualizationPanel.repaint();
                });
            }});
            add(new JCheckBox("Select faces", _visualizationPanel.selectFaces){{
                setPreferredSize(new Dimension(50,20));
                addActionListener((ActionEvent e) -> {
                  _visualizationPanel.selectFaces = isSelected();
                });
            }});
            add(new JCheckBox("Select edges", _visualizationPanel.selectEdges){{
                setPreferredSize(new Dimension(50,20));
                addActionListener((ActionEvent e) -> {
                  _visualizationPanel.selectEdges = isSelected();
                });
            }});
            add(new JCheckBox("Select nodes", _visualizationPanel.selectNodes){{
                setPreferredSize(new Dimension(50,20));
                addActionListener(new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        _visualizationPanel.selectNodes = isSelected();
                    }
                });
            }});
            add(new JButton("Fit to view"){{
                setPreferredSize(new Dimension(50,20));
                addActionListener((ActionEvent e) -> {
                  _visualizationPanel.fitToView();
                });
            }});
        }
    }
    public void addNamedSelection(String nameOfSelection, NamedSelection.TypeOfSelection typeOfSelection, ArrayList<Integer> selectionIndexList){
        _namedSelections.add(new NamedSelection(nameOfSelection, typeOfSelection, selectionIndexList));
    }
    public JPanel getSolidPanel(){
        return _solidPanel;
    }
    public int getNumberOfFaces(){
        return _faces.size();
    }
    public int getNumberOfEdges(){
        return _edges.size();
    }
    public int getNumberOfNodes(){
        return _nodes.size();
    }
    public float calculateDistanceBetweenNodes(int idx_1, int idx_2){
        float dx = _nodes.get(idx_1).get(0) - _nodes.get(idx_2).get(0);
        float dy = _nodes.get(idx_1).get(1) - _nodes.get(idx_2).get(1);
        float dz = _nodes.get(idx_1).get(2) - _nodes.get(idx_2).get(2);
        return (float)Math.sqrt((double)(dx*dx + dy*dy + dz*dz));
    }
    public float getLengthOfEdge(int idx){
        return 0.0f;
    }
    public static class NamedSelection{
        private String _selectionName;
        private TypeOfSelection _selectionType;
        private ArrayList<Integer> _selectionIndexList;

        public enum TypeOfSelection{
            NODE, EDGE, FACE, VOLUME
        }

        public NamedSelection(String selectionName, TypeOfSelection selectionType, ArrayList<Integer> selectionIndexList){
            _selectionName = selectionName;
            _selectionType = selectionType;
            _selectionIndexList = selectionIndexList;
        }
        public String getNamedSelectionName(){
            return _selectionName;
        }
        public TypeOfSelection getNamedSelectionType(){
            return _selectionType;
        }
        public ArrayList<Integer> getNamedSelectionIndexes(){
            return _selectionIndexList;
        }
    }
}