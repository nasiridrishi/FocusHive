export class ExampleService {
  private data: Map<string, any> = new Map();

  async create(id: string, data: any): Promise<void> {
    if (this.data.has(id)) {
      throw new Error('Item already exists');
    }
    this.data.set(id, data);
  }

  async get(id: string): Promise<any> {
    const item = this.data.get(id);
    if (!item) {
      throw new Error('Item not found');
    }
    return item;
  }

  async update(id: string, data: any): Promise<void> {
    if (!this.data.has(id)) {
      throw new Error('Item not found');
    }
    this.data.set(id, data);
  }

  async delete(id: string): Promise<void> {
    if (!this.data.has(id)) {
      throw new Error('Item not found');
    }
    this.data.delete(id);
  }

  async list(): Promise<any[]> {
    return Array.from(this.data.values());
  }
}