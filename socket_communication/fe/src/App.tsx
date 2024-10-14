import React, { useState, useEffect } from 'react';

interface NodeResult {
  node_name: string;
  result: any;
}

interface Style {
  title: string;
  description: string;
}

const LangGraphPage: React.FC = () => {
  const [ws, setWs] = useState<WebSocket | null>(null);
  const [inputValue, setInputValue] = useState<string>('');  // 사용자 입력 값
  const [nodeResults, setNodeResults] = useState<NodeResult[]>([]);  // 각 노드의 결과 저장
  const [styles, setStyles] = useState<Style[]>([]);  // 스타일 선택지
  const [selectedStyles, setSelectedStyles] = useState<number[]>([]);  // 선택된 스타일 인덱스 저장
  const [isGraphCompleted, setIsGraphCompleted] = useState<boolean>(false);  // 그래프 실행 완료 상태

  // WebSocket 연결을 설정하고 그래프 실행 결과를 실시간으로 수신
  const startWebSocket = () => {
    const websocket = new WebSocket('ws://localhost:8000/ws');  // WebSocket 연결 시작
    setWs(websocket);

    websocket.onopen = () => {
      console.log('WebSocket 연결 성공');
      websocket.send(inputValue);  // 사용자 입력값을 서버로 전송
    };

    websocket.onmessage = (event) => {
      const data = JSON.parse(event.data);

      // 노드 결과를 수신하여 저장
      if (data.node_name && data.result) {
        setNodeResults((prevResults) => [...prevResults, data]);  // 각 노드 결과를 저장
      }

      // 스타일 선택지를 수신하여 저장
      if (data.styles) {
        setStyles(data.styles);  // 스타일 선택지 설정
      }

      // 그래프 실행 완료 메시지를 수신
      if (data === 'Graph execution completed') {
        setIsGraphCompleted(true);  // 그래프 실행 완료 상태 업데이트
      }
    };

    websocket.onclose = () => {
      console.log('WebSocket 연결 종료');
    };
  };

  // 사용자 입력 핸들러
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setInputValue(e.target.value);
  };

  // 스타일 선택 핸들러
  const handleStyleSelection = (index: number) => {
    setSelectedStyles((prev) =>
      prev.includes(index) ? prev.filter((i) => i !== index) : [...prev, index]
    );  // 선택된 스타일 업데이트
  };

  // 선택한 스타일을 서버로 전송
  const submitSelectedStyles = () => {
    if (ws && ws.readyState === WebSocket.OPEN) {  // WebSocket이 열려 있는지 확인
      const selectedIndexesString = JSON.stringify(selectedStyles);
      console.log("Submitting selected styles:", selectedIndexesString);  // 디버그용 로그 추가
      ws.send(selectedIndexesString);  // 선택한 스타일의 인덱스를 서버로 전송
    } else {
      console.log("WebSocket is not open. Cannot send selected styles.");
    }
  };


  return (
    <div>
      <h1>LangGraph Execution</h1>

      {/* 사용자 입력을 위한 텍스트 입력 필드 */}
      <input
        type="text"
        value={inputValue}
        onChange={handleInputChange}
        placeholder="Enter your input"
      />
      <button onClick={startWebSocket}>Start WebSocket</button>

      {/* 노드 실행 결과 표시 */}
      <h2>Node Results:</h2>
      <ul>
        {nodeResults.map((result, index) => (
          <li key={index}>
            <strong>{result.node_name}</strong>: {JSON.stringify(result.result)}
          </li>
        ))}
      </ul>

      {/* 스타일 선택 섹션 */}
      {styles.length > 0 && (
        <div>
          <h3>Select Styles:</h3>
          <ul>
            {styles.map((style, index) => (
              <li key={index}>
                <label>
                  <input
                    type="checkbox"
                    value={index}
                    onChange={() => handleStyleSelection(index)}
                    checked={selectedStyles.includes(index)}
                  />
                  {style.title}: {style.description}
                </label>
              </li>
            ))}
          </ul>
          <button onClick={submitSelectedStyles}>Submit Selected Styles</button>
        </div>
      )}

      {/* 그래프 실행 완료 메시지 */}
      {isGraphCompleted && (
        <div>
          <h3>Graph execution completed</h3>
        </div>
      )}
    </div>
  );
};

export default LangGraphPage;
