// src/components/SkillTreeFlow.tsx

import React, { useState, useEffect } from 'react';
import ReactFlow, { MiniMap, Controls, Background, Node, Edge, ReactFlowProvider } from 'react-flow-renderer';
import { skillTree, convertSkillTreeToReactFlow, getAllDescendantIds } from './SkillTree';

const SkillTreeFlow: React.FC = () => {
    const [nodes, setNodes] = useState<Node[]>([]);
    const [edges, setEdges] = useState<Edge[]>([]);
    const [expandedNodes, setExpandedNodes] = useState<Set<string>>(new Set(['1'])); // 초기 확장된 노드 (루트 노드)

    useEffect(() => {
        const { nodes: convertedNodes, edges: convertedEdges } = convertSkillTreeToReactFlow(skillTree, expandedNodes);
        setNodes(convertedNodes);
        setEdges(convertedEdges);
    }, [expandedNodes]);

    const onNodeClick = (_event: React.MouseEvent, node: Node) => {
        const nodeId = node.id;
        setExpandedNodes(prev => {
            const newSet = new Set(prev);
            if (newSet.has(nodeId)) {
                // 노드가 확장된 상태이면 축소
                // 해당 노드와 모든 하위 노드를 제거
                const descendants = getAllDescendantIds(skillTree, nodeId);
                newSet.delete(nodeId);
                descendants.forEach(id => newSet.delete(id));
            } else {
                // 노드가 축소된 상태이면 확장
                newSet.add(nodeId);
            }
            return newSet;
        });
    };

    return (
        <div style={{ width: '100%', height: '100vh' }}>
            <ReactFlowProvider>
                <ReactFlow
                    nodes={nodes}
                    edges={edges}
                    fitView
                    style={{ background: '#B8CEFF' }}
                    onNodeClick={onNodeClick}
                >
                    {/* <MiniMap />
                    <Controls /> */}
                    <Background color="#aaa" gap={16} />
                </ReactFlow>
            </ReactFlowProvider>
        </div>
    );
};

export default SkillTreeFlow;
