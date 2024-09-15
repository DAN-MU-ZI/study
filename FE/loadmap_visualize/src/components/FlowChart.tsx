import React, { useState, useCallback } from 'react';
import ReactFlow, {
    Edge,
    Node,
    Controls,
    Background,
    NodeMouseHandler,
} from 'react-flow-renderer';

// Node 타입 정의
interface SkillNode {
    id: string;
    title: string;
    children: string[];
    parents: string[];
    position: { x: number; y: number };
}

// 노드와 에지 모킹 데이터 (고정된 위치)
const skillNodes: SkillNode[] = [
    { id: 'pf1', title: 'Computer Science Basics', children: ['pf2', 'pf3'], parents: [], position: { x: 250, y: 0 } },
    { id: 'pf2', title: 'Data Structures and Algorithms', children: ['be1', 'db1', 'db2', 'db3', 'db4', 'db5'], parents: ['pf1'], position: { x: 100, y: 150 } },
    { id: 'pf3', title: 'Operating Systems Concepts', children: [], parents: ['pf1'], position: { x: 400, y: 150 } },
    { id: 'be1', title: 'Programming Languages', children: ['be6'], parents: ['pf2'], position: { x: 100, y: 300 } },
    { id: 'be6', title: 'APIs and Microservices', children: ['be11'], parents: ['be1'], position: { x: 100, y: 450 } },
    { id: 'be11', title: 'Web Security', children: [], parents: ['be6'], position: { x: 100, y: 600 } },
    { id: 'db1', title: 'MySQL', children: [], parents: ['pf2'], position: { x: 300, y: 300 } },
    { id: 'db2', title: 'PostgreSQL', children: [], parents: ['pf2'], position: { x: 500, y: 300 } },
    { id: 'db3', title: 'MongoDB', children: [], parents: ['pf2'], position: { x: 300, y: 450 } },
    { id: 'db4', title: 'Cassandra', children: [], parents: ['pf2'], position: { x: 500, y: 450 } },
    { id: 'db5', title: 'Redis', children: [], parents: ['pf2'], position: { x: 400, y: 600 } },
];

// 노드와 에지 생성 함수
const createNodesAndEdges = (hiddenNodes: Set<string>) => {
    const nodes: Node[] = skillNodes
        .filter((node) => !hiddenNodes.has(node.id))
        .map((node) => ({
            id: node.id,
            data: { label: node.title },
            position: node.position, // 고정된 위치 사용
        }));

    const edges: Edge[] = skillNodes
        .flatMap((node) =>
            node.children
                .filter((childId) => !hiddenNodes.has(childId))
                .map((childId) => ({
                    id: `${node.id}-${childId}`,
                    source: node.id,
                    target: childId,
                    animated: true,
                }))
        )
        .filter((edge) => !hiddenNodes.has(edge.source) && !hiddenNodes.has(edge.target));

    return { nodes, edges };
};

const FlowChart: React.FC = () => {
    const [hiddenNodes, setHiddenNodes] = useState<Set<string>>(new Set());

    // 노드 클릭 핸들러
    const onNodeClick: NodeMouseHandler = useCallback((event, node) => {
        setHiddenNodes((prevHiddenNodes) => {
            const newHiddenNodes = new Set(prevHiddenNodes);

            // 토글 방식으로 노드와 자식 노드 숨기기/표시
            if (newHiddenNodes.has(node.id)) {
                // 노드가 이미 숨겨진 경우: 해당 노드와 모든 자식 노드 표시
                newHiddenNodes.delete(node.id);
                skillNodes.forEach((skillNode) => {
                    if (skillNode.parents.includes(node.id)) {
                        newHiddenNodes.delete(skillNode.id);
                    }
                });
            } else {
                // 노드가 숨겨지지 않은 경우: 해당 노드와 모든 자식 노드 숨김
                newHiddenNodes.add(node.id);
                skillNodes.forEach((skillNode) => {
                    if (skillNode.parents.includes(node.id)) {
                        newHiddenNodes.add(skillNode.id);
                    }
                });
            }

            return newHiddenNodes;
        });
    }, []);

    // 노드와 에지 생성
    const { nodes, edges } = createNodesAndEdges(hiddenNodes);

    return (
        <div style={{ width: '100%', height: '100vh' }}>
            <ReactFlow nodes={nodes} edges={edges} onNodeClick={onNodeClick} fitView>
                <Background color="#aaa" gap={16} />
                <Controls />
            </ReactFlow>
        </div>
    );
};

export default FlowChart;
