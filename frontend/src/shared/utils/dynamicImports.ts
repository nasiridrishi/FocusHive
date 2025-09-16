/**
 * Dynamic import utilities for lazy loading heavy libraries
 * Reduces main bundle size by loading libraries on demand
 */

/* eslint-disable @typescript-eslint/no-explicit-any */
// TypeScript has limitations with dynamic imports and can't properly infer named exports
// We use 'any' here to work around this limitation while still achieving code splitting

// Chart library dynamic imports
export const chartLibraries = {
  async recharts() {
    try {
      // Import recharts module - TypeScript can't infer dynamic import types properly
      const rechartsModule = await import('recharts') as any;

      // Return the specific components we need
      return {
        LineChart: rechartsModule.LineChart,
        Line: rechartsModule.Line,
        XAxis: rechartsModule.XAxis,
        YAxis: rechartsModule.YAxis,
        CartesianGrid: rechartsModule.CartesianGrid,
        Tooltip: rechartsModule.Tooltip,
        Legend: rechartsModule.Legend,
        ResponsiveContainer: rechartsModule.ResponsiveContainer,
        BarChart: rechartsModule.BarChart,
        Bar: rechartsModule.Bar,
        PieChart: rechartsModule.PieChart,
        Pie: rechartsModule.Pie,
        Cell: rechartsModule.Cell,
        AreaChart: rechartsModule.AreaChart,
        Area: rechartsModule.Area
      };
    } catch {
      // Debug statement removed
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
    } catch {
      // Debug statement removed
      return {};
    }
  }
};

// Date utility dynamic imports
export const dateLibraries = {
  async dateFns() {
    const [
      {format, parseISO, startOfDay, endOfDay, subDays, addDays},
      {AdapterDateFns}
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
    } catch {
      // Debug statement removed
      return {};
    }
  }
};

// Real-time communication libraries
export const communicationLibraries = {
  async socketIo() {
    const {io} = await import('socket.io-client');
    return {io};
  },

  async webSocket() {
    // Native WebSocket - no import needed, but provides consistent API
    return {WebSocket: window.WebSocket};
  }
};

// State management libraries
export const stateLibraries = {
  async reactQuery() {
    const [
      {QueryClient, useQuery, useMutation, useQueryClient},
      {ReactQueryDevtools}
    ] = await Promise.all([
      import('@tanstack/react-query'),
      import.meta.env.DEV
          ? import('@tanstack/react-query-devtools')
          : Promise.resolve({ReactQueryDevtools: null})
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
      const zustandModule = await import('zustand') as any;
      return {create: zustandModule.create};
    } catch {
      // Debug statement removed
      return {};
    }
  }
};

// Utility libraries
export const utilityLibraries = {
  async lodash() {
    try {
      const lodashModule = await import('lodash-es') as any;
      return {
        debounce: lodashModule.debounce,
        throttle: lodashModule.throttle,
        groupBy: lodashModule.groupBy,
        sortBy: lodashModule.sortBy,
        uniqBy: lodashModule.uniqBy,
        cloneDeep: lodashModule.cloneDeep,
        merge: lodashModule.merge,
        pick: lodashModule.pick,
        omit: lodashModule.omit
      };
    } catch {
      // Debug statement removed
      return {};
    }
  },

  async uuid() {
    try {
      const uuidModule = await import('uuid') as any;
      return {uuidv4: uuidModule.v4, uuidv1: uuidModule.v1};
    } catch {
      // Debug statement removed
      return {};
    }
  },

  async classNames() {
    try {
      const clsx = await import('clsx');
      return {clsx: clsx.default || clsx};
    } catch {
      // Debug statement removed
      return {clsx: (...args: any[]) => args.filter(Boolean).join(' ')};
    }
  }
};

// Music integration libraries
export const musicLibraries = {
  async spotify() {
    try {
      // Note: The package in package.json is '@spotify/web-api-ts-sdk'
      const spotifyModule = await import('@spotify/web-api-ts-sdk') as any;
      return {SpotifyApi: spotifyModule.SpotifyApi};
    } catch {
      // Debug statement removed
      return {SpotifyApi: null};
    }
  }
};

// Preload specific libraries based on user interaction
export const libraryPreloader = {
  preloadCharts: async () => {
    // Debug statement removed
    await Promise.all([
      chartLibraries.muiCharts(),
      chartLibraries.recharts()
    ]);
    // Debug statement removed
  },

  preloadDateUtils: async () => {
    // Debug statement removed
    await dateLibraries.dateFns();
    // Debug statement removed
  },

  preloadCommunication: async () => {
    // Debug statement removed
    await communicationLibraries.socketIo();
    // Debug statement removed
  },

  preloadUtils: async () => {
    // Debug statement removed
    await Promise.all([
      utilityLibraries.lodash(),
      utilityLibraries.uuid(),
      utilityLibraries.classNames()
    ]);
    // Debug statement removed
  }
};

// Smart preloader that considers connection quality
export const smartPreloader = {
  async preloadBasedOnConnection() {
    if ('connection' in navigator) {
      const connection = (navigator as any).connection;
      if (connection) {
        const {effectiveType, saveData} = connection;

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
  logLibraryUsage(_libraryName: string, _size: number) {
    if (import.meta.env.DEV) {
      // console.log(`[Bundle] Loaded ${_libraryName} (~${_size}KB)`);
    }
  },

  getBundleInfo() {
    return {
      charts: {recharts: '~120KB', muiCharts: '~150KB'},
      dates: {dateFns: '~70KB', datePickers: '~100KB'},
      communication: {socketIo: '~30KB'},
      state: {reactQuery: '~45KB', zustand: '~8KB'},
      utils: {lodash: '~65KB', uuid: '~4KB', clsx: '~2KB'},
      music: {spotify: '~40KB'}
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