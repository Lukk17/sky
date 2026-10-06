# state-the-published-path-as-the-served-path

State that the address a caller sends a request to is the address the service serves it on, now that the edge routes
on the real path and rewrites nothing, and correct the sentence in the versioning requirement whose stated reason,
that the public addresses stay `/api/v1/...`, was true of a direct call and false of every route through the edge for
as long as the rewriting existed.
