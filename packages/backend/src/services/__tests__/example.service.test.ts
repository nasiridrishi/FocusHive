import { ExampleService } from '../example.service';

describe('ExampleService', () => {
  let service: ExampleService;

  beforeEach(() => {
    service = new ExampleService();
  });

  describe('create', () => {
    it('should create a new item', async () => {
      await service.create('1', { name: 'Test Item' });
      const item = await service.get('1');
      expect(item).toEqual({ name: 'Test Item' });
    });

    it('should throw error if item already exists', async () => {
      await service.create('1', { name: 'Test Item' });
      await expect(service.create('1', { name: 'Another Item' })).rejects.toThrow(
        'Item already exists'
      );
    });
  });

  describe('get', () => {
    it('should retrieve an existing item', async () => {
      await service.create('1', { name: 'Test Item' });
      const item = await service.get('1');
      expect(item).toEqual({ name: 'Test Item' });
    });

    it('should throw error if item not found', async () => {
      await expect(service.get('nonexistent')).rejects.toThrow('Item not found');
    });
  });

  describe('update', () => {
    it('should update an existing item', async () => {
      await service.create('1', { name: 'Original' });
      await service.update('1', { name: 'Updated' });
      const item = await service.get('1');
      expect(item).toEqual({ name: 'Updated' });
    });

    it('should throw error if item not found', async () => {
      await expect(service.update('nonexistent', { name: 'Test' })).rejects.toThrow(
        'Item not found'
      );
    });
  });

  describe('delete', () => {
    it('should delete an existing item', async () => {
      await service.create('1', { name: 'Test Item' });
      await service.delete('1');
      await expect(service.get('1')).rejects.toThrow('Item not found');
    });

    it('should throw error if item not found', async () => {
      await expect(service.delete('nonexistent')).rejects.toThrow('Item not found');
    });
  });

  describe('list', () => {
    it('should return empty array when no items', async () => {
      const items = await service.list();
      expect(items).toEqual([]);
    });

    it('should return all items', async () => {
      await service.create('1', { name: 'Item 1' });
      await service.create('2', { name: 'Item 2' });
      const items = await service.list();
      expect(items).toHaveLength(2);
      expect(items).toContainEqual({ name: 'Item 1' });
      expect(items).toContainEqual({ name: 'Item 2' });
    });
  });
});