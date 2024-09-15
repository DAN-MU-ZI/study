import React, { useState } from 'react';
import FlowChart from './components/FlowChart';
import SkillTreeFlow from './components/SkillTreeFlow';


const App: React.FC = () => {
  return (
    <div className="flex flex-col h-screen">
      <SkillTreeFlow />
    </div>
  );
};

export default App;
