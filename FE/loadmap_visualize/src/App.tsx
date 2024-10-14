// src/App.tsx
import React, { useEffect, useState, useCallback } from 'react';
import Flow from './Flow';
import './App.css';
import Graph from './Graph';



const App: React.FC = () => {
  return (
    <div >
      <Graph />
    </div>
  );
};

export default App;
