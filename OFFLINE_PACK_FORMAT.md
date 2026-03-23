# Offline Pack Format

PingPlace can import an offline store pack from any direct HTTP URL that returns JSON with this shape:

```json
{
  "region": {
    "id": "texas",
    "displayName": "Texas",
    "updatedAtEpochMillis": 1760000000000,
    "minLatitude": 25.8,
    "maxLatitude": 36.6,
    "minLongitude": -106.7,
    "maxLongitude": -93.5
  },
  "places": [
    {
      "id": "osm:node:123",
      "name": "Whole Foods Market",
      "brand": "Whole Foods",
      "category": "grocery",
      "address": "5100 Belt Line Rd, Dallas, TX",
      "latitude": 32.9523,
      "longitude": -96.8246,
      "searchText": "whole foods market grocery amazon return"
    }
  ]
}
```

Notes:
- `region.id` must be stable. Re-importing the same id replaces that region's existing places.
- `searchText` should be lowercase-friendly searchable text containing brand/category aliases.
- If the region bounds are omitted, PingPlace computes them from the places list.
- The current importer expects plain JSON over HTTP. If you want compressed packs, add a small unpacking layer before import.

Recommended pack-generation pipeline:
1. Download an OSM extract for a city/state/region.
2. Filter to retail/store POIs relevant to PingPlace.
3. Normalize each POI into the fields above.
4. Publish the JSON file at a direct URL.
5. Add the pack entry to a catalog manifest and host both files.
6. Point `OFFLINE_PACK_MANIFEST_URL` at that catalog so the app can discover new packs without an app update.

Catalog manifest shape:

```json
{
  "packs": [
    {
      "id": "dallas-city",
      "displayName": "Dallas",
      "kind": "CITY",
      "region": "Texas",
      "subtitle": "Core Dallas city stores",
      "sourceUrl": "https://example.com/packs/dallas-city.json"
    }
  ]
}
```
