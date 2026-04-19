import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';
import type { RootState } from '../store';

/**
 * Document interface
 */
export interface Document {
  id: number;
  fileName: string;
  originalFileName?: string;
  fileSize: number;
  mimeType: string;
  uploadedAt: string;
  uploadedBy: string;
  status: 'PROCESSING' | 'COMPLETED' | 'FAILED';
  chunkCount?: number;
  tags?: string[];
  summary?: string;
}

/**
 * Pagination interface
 */
export interface Pagination {
  page: number;
  size: number;
  total: number;
  totalPages: number;
}

/**
 * Upload status enum
 */
export type UploadStatus = 'idle' | 'uploading' | 'processing' | 'success' | 'error';

/**
 * Document state interface
 */
interface DocumentState {
  documents: Document[];
  currentDocument: Document | null;
  uploadStatus: UploadStatus;
  uploadProgress: number;
  uploadError: string | null;
  loading: boolean;
  error: string | null;
  pagination: Pagination;
  searchQuery: string;
  filters: {
    status?: string;
    mimeType?: string;
    tags?: string[];
  };
}

/**
 * Initial pagination state
 */
const initialPagination: Pagination = {
  page: 0,
  size: 10,
  total: 0,
  totalPages: 0,
};

/**
 * Initial document state
 */
const initialState: DocumentState = {
  documents: [],
  currentDocument: null,
  uploadStatus: 'idle',
  uploadProgress: 0,
  uploadError: null,
  loading: false,
  error: null,
  pagination: initialPagination,
  searchQuery: '',
  filters: {},
};

/**
 * Fetch documents async thunk
 */
export const fetchDocumentsAsync = createAsyncThunk<
  { documents: Document[]; pagination: Pagination },
  { page?: number; size?: number; query?: string },
  { state: RootState; rejectValue: string }
>(
  'documents/fetchDocumentsAsync',
  async ({ page = 0, size = 10, query = '' }, { rejectWithValue }) => {
    try {
      const token = localStorage.getItem('token');
      const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
      });

      if (query) {
        params.append('query', query);
      }

      const response = await fetch(
        `${import.meta.env.VITE_API_BASE_URL}/documents?${params}`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (!response.ok) {
        throw new Error('Failed to fetch documents');
      }

      const data = await response.json();
      return {
        documents: data.data.content || [],
        pagination: {
          page: data.data.page,
          size: data.data.size,
          total: data.data.totalElements,
          totalPages: data.data.totalPages,
        },
      };
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to fetch documents');
    }
  }
);

/**
 * Upload document async thunk
 */
export const uploadDocumentAsync = createAsyncThunk<
  Document,
  { file: File; tags?: string[] },
  { state: RootState; rejectValue: string }
>(
  'documents/uploadDocumentAsync',
  async ({ file, tags = [] }, { rejectWithValue }) => {
    try {
      const token = localStorage.getItem('token');
      const formData = new FormData();
      formData.append('file', file);

      if (tags.length > 0) {
        tags.forEach((tag) => formData.append('tags', tag));
      }

      const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/documents/upload`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
        },
        body: formData,
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || 'Upload failed');
      }

      const data = await response.json();
      return data.data;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Upload failed');
    }
  }
);

/**
 * Delete document async thunk
 */
export const deleteDocumentAsync = createAsyncThunk<
  string,
  string,
  { state: RootState; rejectValue: string }
>(
  'documents/deleteDocumentAsync',
  async (documentId, { rejectWithValue }) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(
        `${import.meta.env.VITE_API_BASE_URL}/documents/${documentId}`,
        {
          method: 'DELETE',
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (!response.ok) {
        throw new Error('Failed to delete document');
      }

      return documentId;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to delete document');
    }
  }
);

/**
 * Fetch document by ID async thunk
 */
export const fetchDocumentByIdAsync = createAsyncThunk<
  Document,
  string,
  { state: RootState; rejectValue: string }
>(
  'documents/fetchDocumentByIdAsync',
  async (documentId, { rejectWithValue }) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(
        `${import.meta.env.VITE_API_BASE_URL}/documents/${documentId}`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (!response.ok) {
        throw new Error('Failed to fetch document');
      }

      const data = await response.json();
      return data.data;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to fetch document');
    }
  }
);

/**
 * Update document tags async thunk
 */
export const updateDocumentTagsAsync = createAsyncThunk<
  Document,
  { documentId: string; tags: string[] },
  { state: RootState; rejectValue: string }
>(
  'documents/updateDocumentTagsAsync',
  async ({ documentId, tags }, { rejectWithValue }) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(
        `${import.meta.env.VITE_API_BASE_URL}/documents/${documentId}/tags`,
        {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({ tags }),
        }
      );

      if (!response.ok) {
        throw new Error('Failed to update tags');
      }

      const data = await response.json();
      return data.data;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to update tags');
    }
  }
);

/**
 * Document slice
 */
const documentSlice = createSlice({
  name: 'documents',
  initialState,
  reducers: {
    /**
     * Set documents action
     */
    setDocuments: (state, action: PayloadAction<Document[]>) => {
      state.documents = action.payload;
    },

    /**
     * Set current document action
     */
    setCurrentDocument: (state, action: PayloadAction<Document | null>) => {
      state.currentDocument = action.payload;
    },

    /**
     * Set upload status action
     */
    setUploadStatus: (state, action: PayloadAction<UploadStatus>) => {
      state.uploadStatus = action.payload;
    },

    /**
     * Set upload progress action
     */
    setUploadProgress: (state, action: PayloadAction<number>) => {
      state.uploadProgress = action.payload;
    },

    /**
     * Reset upload state action
     */
    resetUploadState: (state) => {
      state.uploadStatus = 'idle';
      state.uploadProgress = 0;
      state.uploadError = null;
    },

    /**
     * Set filters action
     */
    setFilters: (state, action: PayloadAction<Partial<DocumentState['filters']>>) => {
      state.filters = { ...state.filters, ...action.payload };
    },

    /**
     * Clear filters action
     */
    clearFilters: (state) => {
      state.filters = {};
    },

    /**
     * Update document in list action
     */
    updateDocumentInList: (state, action: PayloadAction<Document>) => {
      const index = state.documents.findIndex((doc) => doc.id === action.payload.id);
      if (index !== -1) {
        state.documents[index] = action.payload;
      }
      if (state.currentDocument?.id === action.payload.id) {
        state.currentDocument = action.payload;
      }
    },
  },
  extraReducers: (builder) => {
    // Fetch documents
    builder
      .addCase(fetchDocumentsAsync.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchDocumentsAsync.fulfilled, (state, action) => {
        state.loading = false;
        state.documents = action.payload.documents;
        state.pagination = action.payload.pagination;
      })
      .addCase(fetchDocumentsAsync.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload || 'Failed to fetch documents';
      });

    // Upload document
    builder
      .addCase(uploadDocumentAsync.pending, (state) => {
        state.uploadStatus = 'uploading';
        state.uploadError = null;
      })
      .addCase(uploadDocumentAsync.fulfilled, (state, action) => {
        state.uploadStatus = 'processing';
        state.documents.unshift(action.payload);
        state.pagination.total += 1;
      })
      .addCase(uploadDocumentAsync.rejected, (state, action) => {
        state.uploadStatus = 'error';
        state.uploadError = action.payload || 'Upload failed';
      });

    // Delete document
    builder
      .addCase(deleteDocumentAsync.pending, (state) => {
        state.loading = true;
      })
      .addCase(deleteDocumentAsync.fulfilled, (state, action) => {
        state.loading = false;
        const deletedId = parseInt(action.payload, 10);
        state.documents = state.documents.filter((doc) => doc.id !== deletedId);
        state.pagination.total -= 1;
        if (state.currentDocument?.id === deletedId) {
          state.currentDocument = null;
        }
      })
      .addCase(deleteDocumentAsync.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload || 'Failed to delete document';
      });

    // Fetch document by ID
    builder
      .addCase(fetchDocumentByIdAsync.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchDocumentByIdAsync.fulfilled, (state, action) => {
        state.loading = false;
        state.currentDocument = action.payload;
      })
      .addCase(fetchDocumentByIdAsync.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload || 'Failed to fetch document';
      });

    // Update document tags
    builder
      .addCase(updateDocumentTagsAsync.fulfilled, (state, action) => {
        const index = state.documents.findIndex((doc) => doc.id === action.payload.id);
        if (index !== -1) {
          state.documents[index] = action.payload;
        }
        if (state.currentDocument?.id === action.payload.id) {
          state.currentDocument = action.payload;
        }
      });
  },
});

export const {
  setDocuments,
  setCurrentDocument,
  setUploadStatus,
  setUploadProgress,
  resetUploadState,
  setFilters,
  clearFilters,
  updateDocumentInList,
} = documentSlice.actions;

/**
 * Selectors
 */
export const selectDocuments = (state: RootState) => state.documents.documents;
export const selectCurrentDocument = (state: RootState) => state.documents.currentDocument;
export const selectUploadStatus = (state: RootState) => state.documents.uploadStatus;
export const selectUploadProgress = (state: RootState) => state.documents.uploadProgress;
export const selectUploadError = (state: RootState) => state.documents.uploadError;
export const selectDocumentsLoading = (state: RootState) => state.documents.loading;
export const selectDocumentsError = (state: RootState) => state.documents.error;
export const selectPagination = (state: RootState) => state.documents.pagination;
export const selectSearchQuery = (state: RootState) => state.documents.searchQuery;
export const selectFilters = (state: RootState) => state.documents.filters;

export default documentSlice.reducer;
