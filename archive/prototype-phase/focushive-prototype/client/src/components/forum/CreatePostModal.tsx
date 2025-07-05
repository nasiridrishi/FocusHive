import React, { useState } from 'react';
import type { CreatePostFormData, ForumPost } from '@focushive/shared';

interface CreatePostModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: CreatePostFormData) => void;
}

const TIMEZONES = ['EST', 'CST', 'MST', 'PST', 'GMT', 'CET', 'JST', 'AEST'];
const DAYS = ['monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday'];

const TITLE_SUGGESTIONS = {
  study: [
    'Looking for a study partner for [subject]',
    'Need accountability for exam prep',
    'Study buddy wanted for [course]'
  ],
  work: [
    'Seeking co-working partner for deep focus',
    'Looking for accountability partner - [field]',
    'Remote work buddy needed'
  ],
  accountability: [
    'Need someone to keep me on track',
    'Accountability partner for [goal]',
    'Looking for mutual motivation'
  ],
  group: [
    'Forming study/work group for [topic]',
    'Starting a focus group - join us!',
    'Group accountability sessions'
  ]
};

export const CreatePostModal: React.FC<CreatePostModalProps> = ({ isOpen, onClose, onSubmit }) => {
  const [formData, setFormData] = useState<CreatePostFormData>({
    type: 'study',
    title: '',
    description: '',
    tags: [],
    schedule: {
      days: [],
      timeSlots: [{ start: '09:00', end: '11:00' }],
      timezone: 'EST'
    },
    commitmentLevel: 'weekly',
    workingStyle: {
      videoPreference: 'optional',
      communicationStyle: 'moderate',
      breakPreference: 'synchronized'
    }
  });

  const [tagInput, setTagInput] = useState('');
  const [currentStep, setCurrentStep] = useState(1);

  if (!isOpen) return null;

  const handleTypeSelect = (type: ForumPost['type']) => {
    setFormData({ ...formData, type });
    // Suggest a title
    const suggestions = TITLE_SUGGESTIONS[type];
    if (suggestions && !formData.title) {
      setFormData(prev => ({
        ...prev,
        title: suggestions[0]
      }));
    }
  };

  const handleAddTag = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && tagInput.trim()) {
      e.preventDefault();
      setFormData(prev => ({
        ...prev,
        tags: [...prev.tags, tagInput.trim()]
      }));
      setTagInput('');
    }
  };

  const removeTag = (index: number) => {
    setFormData(prev => ({
      ...prev,
      tags: prev.tags.filter((_, i) => i !== index)
    }));
  };

  const toggleDay = (day: string) => {
    setFormData(prev => ({
      ...prev,
      schedule: {
        ...prev.schedule,
        days: prev.schedule.days.includes(day)
          ? prev.schedule.days.filter(d => d !== day)
          : [...prev.schedule.days, day]
      }
    }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (formData.title && formData.description && formData.tags.length > 0 && formData.schedule.days.length > 0) {
      onSubmit(formData);
      onClose();
    }
  };

  const renderStep1 = () => (
    <div className="space-y-4">
      <h3 className="text-lg font-medium text-gray-900 dark:text-white">What are you looking for?</h3>
      <div className="grid grid-cols-2 gap-4">
        {(['study', 'work', 'accountability', 'group'] as const).map((type) => (
          <button
            key={type}
            onClick={() => {
              handleTypeSelect(type);
              setCurrentStep(2);
            }}
            className={`p-4 border-2 rounded-lg text-left hover:border-indigo-500 dark:hover:border-indigo-400 transition-colors ${
              formData.type === type ? 'border-indigo-500 dark:border-indigo-400 bg-indigo-50 dark:bg-indigo-900/30' : 'border-gray-200 dark:border-gray-600'
            }`}
          >
            <div className="font-medium text-gray-900 dark:text-white capitalize">{type} Buddy</div>
            <div className="text-sm text-gray-500 dark:text-gray-400 mt-1">
              {type === 'study' && 'Find someone to study with'}
              {type === 'work' && 'Co-work and stay productive'}
              {type === 'accountability' && 'Keep each other on track'}
              {type === 'group' && 'Form or join a group'}
            </div>
          </button>
        ))}
      </div>
    </div>
  );

  const renderStep2 = () => (
    <div className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Title</label>
        <input
          type="text"
          value={formData.title}
          onChange={(e) => setFormData({ ...formData, title: e.target.value })}
          className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400"
          placeholder="Be specific about what you're looking for"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Description</label>
        <textarea
          value={formData.description}
          onChange={(e) => setFormData({ ...formData, description: e.target.value })}
          rows={4}
          className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400"
          placeholder="Provide more details about your goals, experience level, and what you hope to achieve..."
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Tags (press Enter to add)</label>
        <input
          type="text"
          value={tagInput}
          onChange={(e) => setTagInput(e.target.value)}
          onKeyDown={handleAddTag}
          className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400"
          placeholder="e.g., JavaScript, MCAT, Thesis Writing"
        />
        <div className="flex flex-wrap gap-2 mt-2">
          {formData.tags.map((tag, index) => (
            <span
              key={index}
              className="px-3 py-1 bg-indigo-100 dark:bg-indigo-900 text-indigo-700 dark:text-indigo-300 rounded-full text-sm flex items-center"
            >
              {tag}
              <button
                onClick={() => removeTag(index)}
                className="ml-2 text-indigo-500 dark:text-indigo-400 hover:text-indigo-700 dark:hover:text-indigo-300"
              >
                ×
              </button>
            </span>
          ))}
        </div>
      </div>

      <button
        onClick={() => setCurrentStep(3)}
        disabled={!formData.title || !formData.description || formData.tags.length === 0}
        className="w-full py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 dark:bg-indigo-500 dark:hover:bg-indigo-600 disabled:bg-gray-300 dark:disabled:bg-gray-600 disabled:cursor-not-allowed"
      >
        Next: Schedule
      </button>
    </div>
  );

  const renderStep3 = () => (
    <div className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Available Days</label>
        <div className="grid grid-cols-7 gap-2">
          {DAYS.map((day) => (
            <button
              key={day}
              onClick={() => toggleDay(day)}
              className={`py-2 px-1 text-xs rounded-md capitalize ${
                formData.schedule.days.includes(day)
                  ? 'bg-indigo-600 text-white dark:bg-indigo-500'
                  : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
              }`}
            >
              {day.slice(0, 3)}
            </button>
          ))}
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Time Slot</label>
        <div className="flex items-center space-x-2">
          <input
            type="time"
            value={formData.schedule.timeSlots[0].start}
            onChange={(e) => setFormData(prev => ({
              ...prev,
              schedule: {
                ...prev.schedule,
                timeSlots: [{ ...prev.schedule.timeSlots[0], start: e.target.value }]
              }
            }))}
            className="px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-md"
          />
          <span className="dark:text-gray-300">to</span>
          <input
            type="time"
            value={formData.schedule.timeSlots[0].end}
            onChange={(e) => setFormData(prev => ({
              ...prev,
              schedule: {
                ...prev.schedule,
                timeSlots: [{ ...prev.schedule.timeSlots[0], end: e.target.value }]
              }
            }))}
            className="px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-md"
          />
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Timezone</label>
        <select
          value={formData.schedule.timezone}
          onChange={(e) => setFormData(prev => ({
            ...prev,
            schedule: { ...prev.schedule, timezone: e.target.value }
          }))}
          className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400"
        >
          {TIMEZONES.map(tz => (
            <option key={tz} value={tz}>{tz}</option>
          ))}
        </select>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Commitment Level</label>
        <select
          value={formData.commitmentLevel}
          onChange={(e) => setFormData({ ...formData, commitmentLevel: e.target.value as any })}
          className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400"
        >
          <option value="one-time">One-time session</option>
          <option value="weekly">Weekly recurring</option>
          <option value="daily">Daily accountability</option>
          <option value="flexible">Flexible</option>
        </select>
      </div>

      <button
        onClick={() => setCurrentStep(4)}
        disabled={formData.schedule.days.length === 0}
        className="w-full py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 dark:bg-indigo-500 dark:hover:bg-indigo-600 disabled:bg-gray-300 dark:disabled:bg-gray-600 disabled:cursor-not-allowed"
      >
        Next: Working Style
      </button>
    </div>
  );

  const renderStep4 = () => (
    <div className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Video Preference</label>
        <select
          value={formData.workingStyle.videoPreference}
          onChange={(e) => setFormData(prev => ({
            ...prev,
            workingStyle: { ...prev.workingStyle, videoPreference: e.target.value as any }
          }))}
          className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400"
        >
          <option value="on">Video on</option>
          <option value="off">Video off</option>
          <option value="optional">Optional</option>
        </select>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Communication Style</label>
        <select
          value={formData.workingStyle.communicationStyle}
          onChange={(e) => setFormData(prev => ({
            ...prev,
            workingStyle: { ...prev.workingStyle, communicationStyle: e.target.value as any }
          }))}
          className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400"
        >
          <option value="minimal">Minimal (focus mode)</option>
          <option value="moderate">Moderate (occasional check-ins)</option>
          <option value="chatty">Chatty (regular interaction)</option>
        </select>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Break Preference</label>
        <select
          value={formData.workingStyle.breakPreference}
          onChange={(e) => setFormData(prev => ({
            ...prev,
            workingStyle: { ...prev.workingStyle, breakPreference: e.target.value as any }
          }))}
          className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400"
        >
          <option value="synchronized">Synchronized breaks</option>
          <option value="independent">Independent breaks</option>
        </select>
      </div>

      <div className="bg-gray-50 dark:bg-gray-700 p-4 rounded-lg">
        <h4 className="font-medium text-gray-900 dark:text-white mb-2">Post Preview</h4>
        <p className="text-sm text-gray-600 dark:text-gray-300">
          <strong>Title:</strong> {formData.title}
        </p>
        <p className="text-sm text-gray-600 dark:text-gray-300">
          <strong>Type:</strong> {formData.type}
        </p>
        <p className="text-sm text-gray-600 dark:text-gray-300">
          <strong>Schedule:</strong> {formData.schedule.days.join(', ')} at {formData.schedule.timeSlots[0].start}-{formData.schedule.timeSlots[0].end} {formData.schedule.timezone}
        </p>
        <p className="text-sm text-gray-600 dark:text-gray-300">
          <strong>Tags:</strong> {formData.tags.join(', ')}
        </p>
      </div>

      <button
        onClick={handleSubmit}
        className="w-full py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700"
      >
        Create Post
      </button>
    </div>
  );

  return (
    <div className="fixed inset-0 bg-gray-500 dark:bg-gray-900 bg-opacity-75 dark:bg-opacity-75 flex items-center justify-center p-4 z-50">
      <div className="bg-white dark:bg-gray-800 rounded-lg max-w-lg w-full p-6">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
            {currentStep === 1 && 'Looking for a Buddy?'}
            {currentStep === 2 && 'Tell us more'}
            {currentStep === 3 && 'When are you available?'}
            {currentStep === 4 && 'Your working style'}
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-500 dark:text-gray-500 dark:hover:text-gray-300"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Progress indicator */}
        <div className="flex items-center justify-center mb-6">
          {[1, 2, 3, 4].map((step) => (
            <React.Fragment key={step}>
              <div
                className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium ${
                  step <= currentStep
                    ? 'bg-indigo-600 text-white'
                    : 'bg-gray-200 dark:bg-gray-700 text-gray-500 dark:text-gray-400'
                }`}
              >
                {step}
              </div>
              {step < 4 && (
                <div
                  className={`w-16 h-1 ${
                    step < currentStep ? 'bg-indigo-600 dark:bg-indigo-500' : 'bg-gray-200 dark:bg-gray-700'
                  }`}
                />
              )}
            </React.Fragment>
          ))}
        </div>

        {currentStep === 1 && renderStep1()}
        {currentStep === 2 && renderStep2()}
        {currentStep === 3 && renderStep3()}
        {currentStep === 4 && renderStep4()}

        {currentStep > 1 && currentStep < 4 && (
          <button
            onClick={() => setCurrentStep(currentStep - 1)}
            className="mt-4 text-sm text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200"
          >
            ← Back
          </button>
        )}
      </div>
    </div>
  );
};