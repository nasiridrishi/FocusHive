/**
 * Dynamic import utilities for lazy loading heavy libraries
 * Reduces main bundle size by loading libraries on demand
 */

// Chart library dynamic imports
export const chartLibraries = {
  async recharts() {
    try {
      const recharts = await import('recharts');
      return {
        LineChart: recharts.LineChart,
        Line: recharts.Line,
        XAxis: recharts.XAxis,
        YAxis: recharts.YAxis,
        CartesianGrid: recharts.CartesianGrid,
        Tooltip: recharts.Tooltip,
        Legend: recharts.Legend,
        ResponsiveContainer: recharts.ResponsiveContainer,
        BarChart: recharts.BarChart,
        Bar: recharts.Bar,
        PieChart: recharts.PieChart,
        Pie: recharts.Pie,
        Cell: recharts.Cell,
        AreaChart: recharts.AreaChart,
        Area: recharts.Area
      };
    } catch (error) {
      console.warn('Recharts not available:', error);
      return {};
    }
  },
  
  async muiCharts() {
    try {
      const [
        lineChart,
        barChart,
        pieChart,
        gauge,
        sparkLineChart
      ] = await Promise.all([
        import('@mui/x-charts/LineChart'),
        import('@mui/x-charts/BarChart'),
        import('@mui/x-charts/PieChart'),
        import('@mui/x-charts/Gauge'),
        import('@mui/x-charts/SparkLineChart')
      ]);
      
      return {
        LineChart: lineChart.LineChart,
        BarChart: barChart.BarChart,
        PieChart: pieChart.PieChart,
        Gauge: gauge.Gauge,
        SparkLineChart: sparkLineChart.SparkLineChart
      };
    } catch (error) {
      console.warn('MUI Charts not available:', error);
      return {};
    }
  }
};

// Date utility dynamic imports
export const dateLibraries = {
  async dateFns() {
    const [
      { format, parseISO, startOfDay, endOfDay, subDays, addDays },
      { AdapterDateFns }
    ] = await Promise.all([
      import('date-fns'),
      import('@mui/x-date-pickers/AdapterDateFns')
    ]);
    
    return {
      format,
      parseISO,
      startOfDay,
      endOfDay,
      subDays,
      addDays,
      AdapterDateFns
    };
  },
  
  async datePickers() {
    try {
      const [
        localizationProvider,
        datePicker,
        timePicker,
        dateTimePicker
      ] = await Promise.all([
        import('@mui/x-date-pickers/LocalizationProvider'),
        import('@mui/x-date-pickers/DatePicker'),
        import('@mui/x-date-pickers/TimePicker'),
        import('@mui/x-date-pickers/DateTimePicker')
      ]);
      
      return {
        LocalizationProvider: localizationProvider.LocalizationProvider,
        DatePicker: datePicker.DatePicker,
        TimePicker: timePicker.TimePicker,
        DateTimePicker: dateTimePicker.DateTimePicker
      };
    } catch (error) {
      console.warn('Date pickers not available:', error);
      return {};
    }
  }
};

// Real-time communication libraries
export const communicationLibraries = {
  async socketIo() {
    const { io } = await import('socket.io-client');
    return { io };
  },
  
  async webSocket() {
    // Native WebSocket - no import needed, but provides consistent API
    return { WebSocket: window.WebSocket };
  }
};

// State management libraries
export const stateLibraries = {
  async reactQuery() {
    const [
      { QueryClient, useQuery, useMutation, useQueryClient },
      { ReactQueryDevtools }
    ] = await Promise.all([
      import('@tanstack/react-query'),
      import.meta.env.DEV 
        ? import('@tanstack/react-query-devtools')
        : Promise.resolve({ ReactQueryDevtools: null })
    ]);
    
    return {
      QueryClient,
      useQuery,
      useMutation,
      useQueryClient,
      ReactQueryDevtools
    };
  },
  
  async zustand() {
    try {
      const zustand = await import('zustand');
      return { create: zustand.create };
    } catch (error) {
      console.warn('Zustand not available:', error);
      return {};
    }
  }
};

// Utility libraries
export const utilityLibraries = {
  async lodash() {
    try {
      const lodash = await import('lodash-es');
      return {
        debounce: lodash.debounce,
        throttle: lodash.throttle,
        groupBy: lodash.groupBy,
        sortBy: lodash.sortBy,
        uniqBy: lodash.uniqBy,
        cloneDeep: lodash.cloneDeep,
        merge: lodash.merge,
        pick: lodash.pick,
        omit: lodash.omit
      };
    } catch (error) {
      console.warn('Lodash not available:', error);
      return {};
    }
  },
  
  async uuid() {
    try {
      const uuid = await import('uuid');
      return { uuidv4: uuid.v4, uuidv1: uuid.v1 };
    } catch (error) {
      console.warn('UUID not available:', error);
      return {};
    }
  },
  
  async classNames() {
    try {
      const clsx = await import('clsx');
      return { clsx: clsx.default || clsx };
    } catch (error) {
      console.warn('clsx not available:', error);
      return { clsx: (...args: any[]) => args.filter(Boolean).join(' ') };
    }
  }
};

// Music integration libraries
export const musicLibraries = {
  async spotify() {
    try {
      const { SpotifyApi } = await import('spotify-web-api-sdk');
      return { SpotifyApi };
    } catch (error) {
      console.warn('Spotify SDK not available:', error);
      return { SpotifyApi: null };
    }
  }
};

// Preload specific libraries based on user interaction
export const libraryPreloader = {
  preloadCharts: async () => {
    console.log('[DynamicImports] Preloading chart libraries...');
    await Promise.all([
      chartLibraries.muiCharts(),
      chartLibraries.recharts()
    ]);
    console.log('[DynamicImports] Chart libraries preloaded');
  },
  
  preloadDateUtils: async () => {
    console.log('[DynamicImports] Preloading date utilities...');
    await dateLibraries.dateFns();
    console.log('[DynamicImports] Date utilities preloaded');
  },
  
  preloadCommunication: async () => {
    console.log('[DynamicImports] Preloading communication libraries...');
    await communicationLibraries.socketIo();
    console.log('[DynamicImports] Communication libraries preloaded');
  },
  
  preloadUtils: async () => {
    console.log('[DynamicImports] Preloading utility libraries...');
    await Promise.all([
      utilityLibraries.lodash(),
      utilityLibraries.uuid(),
      utilityLibraries.classNames()
    ]);
    console.log('[DynamicImports] Utility libraries preloaded');
  }
};

// Smart preloader that considers connection quality
export const smartPreloader = {
  async preloadBasedOnConnection() {
    if ('connection' in navigator) {
      const connection = (navigator as any).connection;
      if (connection) {
        const { effectiveType, saveData } = connection;
        
        // Only preload on good connections
        if (effectiveType === '4g' && !saveData) {
          await libraryPreloader.preloadCharts();
          await libraryPreloader.preloadUtils();
        } else if (effectiveType === '3g' && !saveData) {
          // Only preload essential utilities on 3G
          await utilityLibraries.classNames();
          await utilityLibraries.uuid();
        }
        // Skip preloading on slow connections or save-data mode
      }
    } else {
      // Fallback: preload after a delay if connection info unavailable
      setTimeout(async () => {
        await libraryPreloader.preloadUtils();
      }, 5000);
    }
  }
};

// Bundle size tracking for development
export const bundleTracker = {
  logLibraryUsage(libraryName: string, size: number) {
    if (import.meta.env.DEV) {
      console.log(`[Bundle] Loaded ${libraryName} (~${size}KB)`);
    }
  },
  
  getBundleInfo() {
    return {
      charts: { recharts: '~120KB', muiCharts: '~150KB' },
      dates: { dateFns: '~70KB', datePickers: '~100KB' },
      communication: { socketIo: '~30KB' },
      state: { reactQuery: '~45KB', zustand: '~8KB' },
      utils: { lodash: '~65KB', uuid: '~4KB', clsx: '~2KB' },
      music: { spotify: '~40KB' }
    };
  }
};

export default {
  charts: chartLibraries,
  dates: dateLibraries,
  communication: communicationLibraries,
  state: stateLibraries,
  utils: utilityLibraries,
  music: musicLibraries,
  preloader: libraryPreloader,
  smartPreloader,
  bundleTracker
};