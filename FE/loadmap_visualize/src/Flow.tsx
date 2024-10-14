import { useState, useCallback } from 'react';
import {
    ReactFlow,
    addEdge,
    applyNodeChanges,
    applyEdgeChanges,
    Node,
    Edge,
    NodeChange,
    EdgeChange,
} from '@xyflow/react';

const initialNodes: Node[] = [
    { id: '1', data: { label: 'Node 1' }, position: { x: 0, y: 0 } },
    { id: '2', data: { label: 'Node 2' }, position: { x: 100, y: 100 } }
];

const initialEdges: Edge[] = [{ id: 'e1-2', source: '1', target: '2' }];

const Flow = () => {
    const [nodes, setNodes] = useState(initialNodes);
    const [edges, setEdges] = useState(initialEdges);

    const onNodesChange = useCallback(
        (changes: NodeChange<Node>[]) => setNodes((nds) => applyNodeChanges(changes, nds)),
        []
    );

    const onEdgesChange = useCallback(
        (changes: EdgeChange<Edge>[]) => setEdges((eds) => applyEdgeChanges(changes, eds)),
        []
    );

    const onConnect = useCallback(
        (connection: any) => setEdges((eds) => addEdge(connection, eds)),
        []
    );

    return (
        <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
        />
    );
};
export default Flow;
