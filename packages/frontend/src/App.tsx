import { Button } from './components/Button';

function App() {
  return (
    <div className="min-h-screen bg-gray-100 flex items-center justify-center">
      <div className="bg-white p-8 rounded-lg shadow-md">
        <h1 className="text-3xl font-bold text-gray-800 mb-4">
          Welcome to FocusHive
        </h1>
        <p className="text-gray-600 mb-6">
          Your digital co-working and co-studying platform
        </p>
        <Button onClick={() => console.log('Getting started!')}>
          Get Started
        </Button>
      </div>
    </div>
  );
}

export default App;