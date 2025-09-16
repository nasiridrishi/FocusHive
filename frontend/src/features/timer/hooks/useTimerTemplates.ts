import * as React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { timerService } from '../services/timerService';
import type {
  TimerTemplate,
  CreateTimerTemplateRequest,
  UpdateTimerTemplateRequest,
  TimerTemplatesResponse
} from '@/contracts/timer';

/**
 * Hook for managing timer templates
 * Provides CRUD operations for timer templates
 */
export function useTimerTemplates() {
  const queryClient = useQueryClient();

  // Query for all templates
  const {
    data: templatesResponse,
    isLoading,
    error,
    refetch
  } = useQuery({
    queryKey: ['timer', 'templates'],
    queryFn: () => timerService.getTimerTemplates(),
    staleTime: 5 * 60 * 1000, // Consider data stale after 5 minutes
    gcTime: 10 * 60 * 1000, // Keep in cache for 10 minutes
  });

  // Extract templates from response
  const templates = React.useMemo(() => {
    if (!templatesResponse) return [];
    if (Array.isArray(templatesResponse)) return templatesResponse;

    // Handle TimerTemplatesResponse structure
    const allTemplates: TimerTemplate[] = [];
    if (templatesResponse.builtIn) allTemplates.push(...templatesResponse.builtIn);
    if (templatesResponse.custom) allTemplates.push(...templatesResponse.custom);
    if (templatesResponse.recent) allTemplates.push(...templatesResponse.recent);

    // Remove duplicates based on id
    const uniqueTemplates = Array.from(
      new Map(allTemplates.map(t => [t.id, t])).values()
    );

    return uniqueTemplates;
  }, [templatesResponse]);

  return {
    templates,
    isLoading,
    error,
    refetch,
  };
}

/**
 * Hook for getting a single timer template
 */
export function useTimerTemplate(templateId: number | undefined) {
  return useQuery({
    queryKey: ['timer', 'templates', templateId],
    queryFn: () => templateId ? timerService.getTimerTemplate(templateId) : null,
    enabled: !!templateId,
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
}

/**
 * Hook for creating a timer template
 */
export function useCreateTimerTemplate() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (template: CreateTimerTemplateRequest) =>
      timerService.createTimerTemplate(template),
    onSuccess: (newTemplate) => {
      // Add new template to cache
      queryClient.setQueryData<TimerTemplate[]>(
        ['timer', 'templates'],
        (old = []) => [...old, newTemplate]
      );

      // Invalidate to ensure fresh data
      queryClient.invalidateQueries({ queryKey: ['timer', 'templates'] });
    },
    onError: (error) => {
      console.error('Failed to create timer template:', error);
    },
  });
}

/**
 * Hook for updating a timer template
 */
export function useUpdateTimerTemplate() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, updates }: { id: number; updates: UpdateTimerTemplateRequest }) =>
      timerService.updateTimerTemplate(id, updates),
    onSuccess: (updatedTemplate) => {
      // Update template in list cache
      queryClient.setQueryData<TimerTemplate[]>(
        ['timer', 'templates'],
        (old = []) => old.map(t => t.id === updatedTemplate.id ? updatedTemplate : t)
      );

      // Update individual template cache
      queryClient.setQueryData(
        ['timer', 'templates', updatedTemplate.id],
        updatedTemplate
      );

      // Invalidate to ensure fresh data
      queryClient.invalidateQueries({ queryKey: ['timer', 'templates'] });
    },
    onError: (error) => {
      console.error('Failed to update timer template:', error);
    },
  });
}

/**
 * Hook for deleting a timer template
 */
export function useDeleteTimerTemplate() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (templateId: number | string) => timerService.deleteTimerTemplate(String(templateId)),
    onSuccess: (_, templateId) => {
      // Remove template from cache
      queryClient.setQueryData<TimerTemplate[]>(
        ['timer', 'templates'],
        (old = []) => old.filter(t => t.id !== templateId)
      );

      // Remove individual template cache
      queryClient.removeQueries({ queryKey: ['timer', 'templates', templateId] });

      // Invalidate to ensure fresh data
      queryClient.invalidateQueries({ queryKey: ['timer', 'templates'] });
    },
    onError: (error) => {
      console.error('Failed to delete timer template:', error);
    },
  });
}

/**
 * Hook for using a template to start a timer session
 */
export function useStartTimerFromTemplate() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ templateId, hiveId }: { templateId: number; hiveId?: number }) =>
      timerService.startTimerFromTemplate(templateId, hiveId),
    onSuccess: (session) => {
      // Update active session
      queryClient.setQueryData(['timer', 'active'], session);

      // Invalidate history to include new session
      queryClient.invalidateQueries({ queryKey: ['timer', 'history'] });
    },
    onError: (error) => {
      console.error('Failed to start timer from template:', error);
    },
  });
}

/**
 * Hook for favorite timer templates
 */
export function useFavoriteTemplates() {
  const { templates, isLoading } = useTimerTemplates();

  const favoriteTemplates = templates.filter((t: TimerTemplate) => t.isFavorite);
  const recentTemplates = [...templates]
    .sort((a: TimerTemplate, b: TimerTemplate) =>
      new Date(b.lastUsedAt || 0).getTime() - new Date(a.lastUsedAt || 0).getTime()
    )
    .slice(0, 5);

  return {
    favoriteTemplates,
    recentTemplates,
    isLoading,
  };
}

/**
 * Hook for toggling favorite status of a template
 */
export function useToggleTemplateFavorite() {
  const updateTemplate = useUpdateTimerTemplate();

  return useMutation({
    mutationFn: ({ templateId, isFavorite }: { templateId: number; isFavorite: boolean }) =>
      updateTemplate.mutateAsync({
        id: templateId,
        updates: { isFavorite }
      }),
  });
}

/**
 * Hook for template categories
 */
export function useTemplateCategories() {
  const { templates } = useTimerTemplates();

  // Extract unique categories from templates
  const categories = Array.from(
    new Set(templates.map((t: TimerTemplate) => t.category).filter(Boolean))
  );

  const templatesByCategory = categories.reduce((acc, category) => {
    acc[category!] = templates.filter((t: TimerTemplate) => t.category === category);
    return acc;
  }, {} as Record<string, TimerTemplate[]>);

  return {
    categories,
    templatesByCategory,
    uncategorized: templates.filter((t: TimerTemplate) => !t.category),
  };
}

/**
 * Hook for template quick actions
 */
export function useTimerTemplateActions() {
  const createTemplate = useCreateTimerTemplate();
  const updateTemplate = useUpdateTimerTemplate();
  const deleteTemplate = useDeleteTimerTemplate();
  const startFromTemplate = useStartTimerFromTemplate();
  const toggleFavorite = useToggleTemplateFavorite();

  return {
    create: createTemplate.mutate,
    update: updateTemplate.mutate,
    delete: deleteTemplate.mutate,
    startTimer: startFromTemplate.mutate,
    toggleFavorite: toggleFavorite.mutate,

    isCreating: createTemplate.isPending,
    isUpdating: updateTemplate.isPending,
    isDeleting: deleteTemplate.isPending,
    isStarting: startFromTemplate.isPending,
    isTogglingFavorite: toggleFavorite.isPending,
  };
}