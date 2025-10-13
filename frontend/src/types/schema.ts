export type SchemaFieldType =
  | 'STRING'
  | 'NUMBER'
  | 'BOOLEAN'
  | 'DATE'
  | 'ARRAY'
  | 'OBJECT'
  | 'NULL'
  | 'UNKNOWN';

export interface SchemaField {
  name: string;
  type: SchemaFieldType;
  required: boolean;
  nullable: boolean;
  arrayItemType?: SchemaFieldType | null;
}

export interface SchemaResult {
  collection: string;
  sampleSize: number;
  generatedAt: string;
  fields: Record<string, SchemaField>;
}

