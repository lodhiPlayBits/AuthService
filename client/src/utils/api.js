/**
 * API utility functions for making authenticated requests
 */

/**
 * Get the authentication headers including the JWT token
 * @returns {Object} Headers object with Authorization header
 */
export const getAuthHeaders = () => {
  const token = localStorage.getItem('token');
  const headers = {
    'Content-Type': 'application/json',
  };
  
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  
  return headers;
};

/**
 * Make an authenticated fetch request
 * @param {string} url - The API endpoint URL
 * @param {Object} options - Fetch options (method, body, etc.)
 * @returns {Promise<Response>} The fetch response
 */
export const authenticatedFetch = async (url, options = {}) => {
  const headers = {
    ...getAuthHeaders(),
    ...options.headers,
  };
  
  return fetch(url, {
    ...options,
    headers,
    credentials: 'include', // Important: send cookies with request
  });
};

/**
 * Make a GET request with authentication
 * @param {string} url - The API endpoint URL
 * @returns {Promise<Response>} The fetch response
 */
export const authGet = (url) => {
  return authenticatedFetch(url, { method: 'GET' });
};

/**
 * Make a POST request with authentication
 * @param {string} url - The API endpoint URL
 * @param {Object} body - Request body
 * @returns {Promise<Response>} The fetch response
 */
export const authPost = (url, body) => {
  return authenticatedFetch(url, {
    method: 'POST',
    body: JSON.stringify(body),
  });
};

/**
 * Make a PUT request with authentication
 * @param {string} url - The API endpoint URL
 * @param {Object} body - Request body (optional)
 * @returns {Promise<Response>} The fetch response
 */
export const authPut = (url, body = null) => {
  const options = { method: 'PUT' };
  if (body) {
    options.body = JSON.stringify(body);
  }
  return authenticatedFetch(url, options);
};

/**
 * Make a DELETE request with authentication
 * @param {string} url - The API endpoint URL
 * @returns {Promise<Response>} The fetch response
 */
export const authDelete = (url) => {
  return authenticatedFetch(url, { method: 'DELETE' });
};
