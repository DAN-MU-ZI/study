import React, { useState, useEffect, useCallback } from 'react';
import {
    ReactFlow,
    addEdge,
    applyNodeChanges,
    applyEdgeChanges,
    type Node,
    type Edge,
    type FitViewOptions,
    type OnConnect,
    type OnNodesChange,
    type OnEdgesChange,
    type NodeMouseHandler,
    type OnNodeDrag,
    type NodeTypes,
    type DefaultEdgeOptions,
    useNodesState,
    useEdgesState,
    getIncomers,
    getOutgoers,
    getConnectedEdges,
} from '@xyflow/react';

// API 모킹 데이터
const data = {
    nodes: [
        { id: "1", type: "MajorCategory", name: "Computer Science" },
        { id: "2", type: "SubCategory", name: "Programming Languages" },
        { id: "3", type: "DetailedCurriculum", name: "TypeScript Basics" },
        { id: "4", type: "SpecificLearningItem", name: "Interfaces in TypeScript" },
        { id: "5", type: "LearningPath", name: "Web Development Path" },
        { id: "6", type: "Tag", name: "TypeScript" },
    ],
    edges: [
        { id: "e1", type: "Hierarchy", source: "1", target: "2" },
        { id: "e2", type: "Hierarchy", source: "2", target: "3" },
        { id: "e3", type: "Hierarchy", source: "3", target: "4" },
        { id: "e4", type: "Prerequisite", source: "4", target: "3" },
        { id: "e5", type: "Tagging", source: "4", target: "6" },
        { id: "e6", type: "Choice", source: "5", target: "3" },
    ],
};

// 데이터 변환 함수
type NodeType = 'MajorCategory' | 'SubCategory' | 'DetailedCurriculum' | 'SpecificLearningItem' | 'LearningPath' | 'Tag';

function createGraphFromData(nodesData: any[], edgesData: any[]) {
    const positions: Record<NodeType, { x: number, y: number }> = {
        MajorCategory: { x: 100, y: 50 },
        SubCategory: { x: 200, y: 150 },
        DetailedCurriculum: { x: 300, y: 250 },
        SpecificLearningItem: { x: 400, y: 350 },
        LearningPath: { x: 500, y: 450 },
        Tag: { x: 600, y: 550 },
    };

    const nodes = nodesData.map(node => {
        const nodeType = node.type as NodeType; // 타입 캐스팅
        return {
            id: node.id,
            // type: nodeType,
            data: { label: node.name },
            position: positions[nodeType] || { x: Math.random() * 500, y: Math.random() * 500 },  // 지정된 위치가 없으면 임의 위치 설정
        };
    });

    const edges = edgesData.map(edge => ({
        id: edge.id,
        source: edge.source,
        target: edge.target,
        // type: edge.type || 'default',  // 엣지 타입이 없으면 기본값으로 설정
        animated: true, // 애니메이션을 통해 엣지가 잘 보이도록 설정 (선택 사항)
    }));

    return { nodes, edges };
}

// React Flow 컴포넌트
function Graph() {
    const [nodes, setNodes] = useNodesState<Node>([]);
    const [edges, setEdges] = useEdgesState<Edge>([]);

    // 컴포넌트가 마운트될 때 mock 데이터를 기반으로 노드와 엣지 설정
    useEffect(() => {
        const graph = createGraphFromData(data.nodes, data.edges);
        setNodes(graph.nodes);
        setEdges(graph.edges);
    }, []);

    // 노드 및 엣지 변경 처리
    const onNodesChange: OnNodesChange = useCallback(
        (changes) => setNodes((nds) => applyNodeChanges(changes, nds)),
        [setNodes],
    );
    const onEdgesChange: OnEdgesChange = useCallback(
        (changes) => setEdges((eds) => applyEdgeChanges(changes, eds)),
        [setEdges],
    );
    const onConnect: OnConnect = useCallback(
        (connection) => setEdges((eds) => addEdge(connection, eds)),
        [setEdges],
    );

    const onNodeClick: NodeMouseHandler = useCallback(
        (event, node: Node) => {
            const incomers = getIncomers(node, nodes, edges);
            const outgoers = getOutgoers(node, nodes, edges);
            const connectedEdges = getConnectedEdges([node], edges);
            console.log('Node:', node, 'Incomers:', incomers, 'Outgoers:', outgoers, 'ConnectedEdges:', connectedEdges);
        },
        [nodes, edges]
    );

    // React Flow UI 렌더링
    return (
        <div style={{ height: '500px' }}>
            <ReactFlow
                nodes={nodes}
                edges={edges}
                onNodesChange={onNodesChange}
                onNodeClick={onNodeClick}
                onEdgesChange={onEdgesChange}
                onConnect={onConnect}
                fitView
            />
        </div>
    );
}

export default Graph;
